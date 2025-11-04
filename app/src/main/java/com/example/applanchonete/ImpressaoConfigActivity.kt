package com.example.applanchonete

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

class ImpressaoConfigActivity : AppCompatActivity() {

    private val TAG = "ImpressaoConfig"
    private val CHAVE_IMPRESSORA_BT = "impressora_bluetooth_address"
    private lateinit var tvImpressoraSelecionada: TextView
    private lateinit var btnSelecionarImpressora: Button
    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var sharedPreferences: SharedPreferences
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var granted = true
        permissions.entries.forEach {
            if (!it.value) granted = false
        }
        if (granted) {
            Log.d(TAG, "Permissões Bluetooth concedidas.")
            mostrarListaImpressorasPareadas()
        } else {
            Log.w(TAG, "Permissões Bluetooth negadas.")
            Toast.makeText(this, "Permissões Bluetooth são necessárias para selecionar a impressora.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_impressao_config)

        sharedPreferences = getSharedPreferences("AppLanchonetePrefs", Context.MODE_PRIVATE)
        tvImpressoraSelecionada = findViewById(R.id.tvImpressoraSelecionada)
        btnSelecionarImpressora = findViewById(R.id.btnSelecionarImpressora)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        carregarImpressoraSalva()

        btnSelecionarImpressora.setOnClickListener {
            if (verificarPermissoesBluetooth()) {
                mostrarListaImpressorasPareadas()
            } else {
                pedirPermissoesBluetooth()
            }
        }
    }

    private fun carregarImpressoraSalva() {
        val enderecoSalvo = sharedPreferences.getString(CHAVE_IMPRESSORA_BT, null)
        if (enderecoSalvo != null) {
            if (verificarPermissoesBluetooth(checkOnlyConnect = true)) {
                try {
                    val device = bluetoothAdapter?.getRemoteDevice(enderecoSalvo)
                    @SuppressLint("MissingPermission")
                    val nomeDevice = device?.name ?: "Nome Desconhecido"
                    tvImpressoraSelecionada.text = "$nomeDevice\n($enderecoSalvo)"
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException ao obter nome do dispositivo: ${e.message}")
                    tvImpressoraSelecionada.text = "Erro de permissão\n($enderecoSalvo)"
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Endereço Bluetooth salvo inválido: $enderecoSalvo")
                    tvImpressoraSelecionada.text = "Endereço inválido\n($enderecoSalvo)"
                    salvarImpressoraSelecionada(null, null)
                }
            } else {
                tvImpressoraSelecionada.text = "Permissão necessária\n($enderecoSalvo)"
            }
        } else {
            tvImpressoraSelecionada.text = "Nenhuma impressora selecionada"
        }
    }

    private fun verificarPermissoesBluetooth(checkOnlyConnect: Boolean = false): Boolean {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth não suportado neste dispositivo.", Toast.LENGTH_SHORT).show()
            return false
        }

        val permissoesNecessarias = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!checkOnlyConnect) {
                permissoesNecessarias.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            permissoesNecessarias.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissoesNecessarias.add(Manifest.permission.BLUETOOTH)
            permissoesNecessarias.add(Manifest.permission.BLUETOOTH_ADMIN)
            permissoesNecessarias.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        return permissoesNecessarias.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun pedirPermissoesBluetooth() {
        val permissoesAPedir = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissoesAPedir.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissoesAPedir.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissoesAPedir.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissoesAPedir.isNotEmpty()) {
            requestPermissionLauncher.launch(permissoesAPedir.toTypedArray())
        } else {
            Log.d(TAG, "Tentou pedir permissões, mas nenhuma parecia necessária.")
            mostrarListaImpressorasPareadas()
        }
    }

    @SuppressLint("MissingPermission")
    private fun mostrarListaImpressorasPareadas() {
        if (!verificarPermissoesBluetooth()) {
            Log.e(TAG, "Tentativa de mostrar lista sem permissão!")
            pedirPermissoesBluetooth()
            return
        }

        if (bluetoothAdapter?.isEnabled == false) {
            Toast.makeText(this, "Por favor, ligue o Bluetooth.", Toast.LENGTH_SHORT).show()
            return
        }

        val dispositivosPareados: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val listaNomes = mutableListOf<String>()
        val listaDevices = mutableListOf<BluetoothDevice>()

        if (dispositivosPareados.isNullOrEmpty()) {
            Toast.makeText(this, "Nenhum dispositivo Bluetooth pareado encontrado.", Toast.LENGTH_SHORT).show()
            return
        }

        dispositivosPareados.forEach { device ->
            listaNomes.add("${device.name ?: "Nome Desconhecido"} (${device.address})")
            listaDevices.add(device)
            Log.d(TAG, "Dispositivo Pareado: ${device.name} - ${device.address}")
        }
        AlertDialog.Builder(this)
            .setTitle("Selecione a Impressora")
            .setItems(listaNomes.toTypedArray()) { _, which ->
                val deviceSelecionado = listaDevices[which]
                salvarImpressoraSelecionada(deviceSelecionado.name, deviceSelecionado.address)
                carregarImpressoraSalva()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun salvarImpressoraSelecionada(nome: String?, endereco: String?) {
        with(sharedPreferences.edit()) {
            putString(CHAVE_IMPRESSORA_BT, endereco)
            putString("impressora_bluetooth_name", nome)
            apply()
        }
        Log.i(TAG, "Impressora salva: Nome=$nome, Endereço=$endereco")
    }
}