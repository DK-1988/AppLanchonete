package com.example.applanchonete

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.IOException
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await

data class CarrinhoUiState(
    val itens: List<ItemVenda> = emptyList(),
    val total: Double = 0.0
)

class VendasViewModelRefatorado(application: Application) : AndroidViewModel(application) {

    private val TAG = "VendasVMRefactor"
    private val db = FirebaseFirestore.getInstance()
    private val ctx: Context = application.applicationContext

    private val _carrinho = MutableStateFlow<List<ItemVenda>>(emptyList())
    val carrinho: StateFlow<List<ItemVenda>> = _carrinho.asStateFlow()

    val carrinhoUiState: StateFlow<CarrinhoUiState> = _carrinho.map { itens ->
        val total = itens.sumOf { it.totalItem }
        CarrinhoUiState(itens = itens, total = total)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CarrinhoUiState())

    private val _progressoTexto = MutableStateFlow<String?>(null)
    val progressoTexto: StateFlow<String?> = _progressoTexto.asStateFlow()

    private val _erro = MutableSharedFlow<String>(replay = 1)
    val erro: SharedFlow<String> = _erro.asSharedFlow()

    private val _sucesso = MutableSharedFlow<Unit>(replay = 1)
    val sucesso: SharedFlow<Unit> = _sucesso.asSharedFlow()

    private val _impressaoConcluida = MutableSharedFlow<Unit>(replay = 1)
    val impressaoConcluida: SharedFlow<Unit> = _impressaoConcluida.asSharedFlow()

    private var clienteIdInterno: String = ""
    private var clienteNomeInterno: String = ""
    private var sessaoCaixaIdInterno: String = ""

    fun adicionarOuMesclarItem(novoItem: ItemVenda) {
        val normalizedKey = gerarChaveItem(novoItem)
        updateCarrinho { lista ->
            val idx = lista.indexOfFirst { gerarChaveItem(it) == normalizedKey }
            if (idx >= 0) {
                val mutable = lista.toMutableList()
                val existente = mutable[idx].copy()
                existente.quantidade += novoItem.quantidade
                existente.totalItem = (existente.precoUnitario + existente.precoAdicionais) * existente.quantidade
                mutable[idx] = existente
                mutable.toList()
            } else {
                (lista + novoItem).toList()
            }
        }
    }

    fun aumentarQuantidade(index: Int) {
        updateCarrinho { lista ->
            if (index < 0 || index >= lista.size) return@updateCarrinho lista
            val mutable = lista.toMutableList()
            val it = mutable[index].copy()
            it.quantidade++
            it.totalItem = (it.precoUnitario + it.precoAdicionais) * it.quantidade
            mutable[index] = it
            mutable.toList()
        }
    }

    fun diminuirQuantidade(index: Int) {
        updateCarrinho { lista ->
            if (index < 0 || index >= lista.size) return@updateCarrinho lista
            val mutable = lista.toMutableList()
            val it = mutable[index].copy()
            if (it.quantidade > 1) {
                it.quantidade--
                it.totalItem = (it.precoUnitario + it.precoAdicionais) * it.quantidade
                mutable[index] = it
            } else {
                mutable.removeAt(index)
            }
            mutable.toList()
        }
    }

    fun removerItem(index: Int) {
        updateCarrinho { lista ->
            if (index < 0 || index >= lista.size) return@updateCarrinho lista
            val mutable = lista.toMutableList()
            mutable.removeAt(index)
            mutable.toList()
        }
    }

    fun limparCarrinho() {
        _carrinho.value = emptyList()
    }

    private fun updateCarrinho(mutator: (List<ItemVenda>) -> List<ItemVenda>) {
        _carrinho.update { current -> mutator(current) }
    }

    private fun gerarChaveItem(item: ItemVenda): String {
        val opChave = item.opcoesSelecionadas
            .map { it.nome }
            .sorted()
            .joinToString("|")
        return "${item.produtoId}::${opChave}"
    }

    fun processarVenda(
        clienteId: String,
        clienteNome: String,
        sessaoCaixaId: String,
        listaPagamentos: List<Pagamento>,
        imprimir: Boolean
    ) {
        val itensSnapshot = _carrinho.value.toList()
        if (itensSnapshot.isEmpty()) {
            viewModelScope.launch { _erro.emit("O carrinho está vazio.") }
            return
        }

        clienteIdInterno = clienteId
        clienteNomeInterno = clienteNome
        sessaoCaixaIdInterno = sessaoCaixaId

        viewModelScope.launch {
            try {
                _progressoTexto.value = "Preparando venda..."
                val totalVenda = itensSnapshot.sumOf { it.totalItem }
                val totalCusto = itensSnapshot.sumOf { it.precoCustoUnitario * it.quantidade }
                val lucroTotal = totalVenda - totalCusto

                var venda = Venda( // CORRIGIDO: val -> var
                    itens = itensSnapshot,
                    totalVenda = totalVenda,
                    pagamentos = listaPagamentos,
                    sessaoCaixaId = sessaoCaixaId,
                    totalCusto = totalCusto,
                    lucroTotal = lucroTotal,
                    clienteId = clienteId,
                    clienteNome = clienteNome,
                    dataHora = Timestamp.now()
                )

                _progressoTexto.value = "Salvando venda..."
                val docRef = db.collection("vendas").add(venda).await()
                venda.id = docRef.id // Agora é válido
                Log.d(TAG, "Venda salva com ID: ${venda.id}")

                if (sessaoCaixaId.isNotBlank()) {
                    try {
                        db.collection("sessoes_caixa")
                            .document(sessaoCaixaId)
                            .update("valorTotalVendas", FieldValue.increment(venda.totalVenda))
                            .await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Falha ao incrementar valorTotalVendas: ${e.message}", e)
                    }
                }

                _progressoTexto.value = "Atualizando estoque..."
                atualizarEstoqueEmBatches(venda)

                _progressoTexto.value = "Carregando dados da empresa..."
                val empresaInfo = buscarEmpresaConfig()

                _sucesso.emit(Unit)

                if (imprimir) {
                    _progressoTexto.value = "Preparando impressão..."
                    val prefs: SharedPreferences = ctx.getSharedPreferences("AppLanchonetePrefs", Context.MODE_PRIVATE)
                    val address = prefs.getString("impressora_bluetooth_address", null)
                    if (address.isNullOrBlank()) {
                        Log.i(TAG, "Nenhuma impressora configurada, pulando impressão.")
                        _erro.emit("Impressão pulada: Impressora não configurada.")
                        _impressaoConcluida.emit(Unit)
                    } else {
                        val texto = formatarReciboTexto(empresaInfo, venda)
                        val resultado = withContext(Dispatchers.IO) {
                            BluetoothPrinter.printToBluetooth(ctx, address, texto.toByteArray(), timeoutMs = 15000L)
                        }
                        if (resultado.isSuccess) {
                            _progressoTexto.value = "Comprovante enviado!"
                        } else {
                            val msg = resultado.exceptionOrNull()?.message ?: "Erro ao imprimir"
                            _erro.emit(msg)
                        }
                        _impressaoConcluida.emit(Unit)
                    }
                } else {
                    Log.i(TAG, "Impressão pulada por parâmetro.")
                    _impressaoConcluida.emit(Unit)
                }

                limparCarrinho()
            } catch (e: Exception) {
                Log.e(TAG, "Erro processando venda:", e)
                _erro.emit(e.message ?: "Erro desconhecido ao processar venda.")
            } finally {
                _progressoTexto.value = null
                clienteIdInterno = ""
                clienteNomeInterno = ""
                sessaoCaixaIdInterno = ""
            }
        }
    }

    private suspend fun buscarEmpresaConfig(): EmpresaInfo {
        return try {
            val snap = db.collection("configuracoes").document("empresa").get().await()
            snap.toObject(EmpresaInfo::class.java) ?: EmpresaInfo()
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao buscar config empresa: ${e.message}", e)
            EmpresaInfo()
        }
    }

    private suspend fun atualizarEstoqueEmBatches(venda: Venda) {
        try {
            val ops = venda.itens
                .filter { it.produtoId.isNotBlank() }
                .map { item ->
                    Pair(db.collection("produtos").document(item.produtoId), -item.quantidade.toLong())
                }

            val chunked = ops.chunked(500)
            for (grupo in chunked) {
                val batch = db.batch()
                for ((ref, dec) in grupo) {
                    batch.update(ref, "quantidadeEstoque", FieldValue.increment(dec))
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar estoque em batches: ${e.message}", e)
            _erro.emit("Erro ao atualizar estoque: ${e.message ?: "desconhecido"}")
        }
    }

    private fun formatarReciboTexto(empresa: EmpresaInfo, venda: Venda): String {
        val fMoeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        if (!empresa.nomeFantasia.isNullOrBlank()) {
            sb.appendLine("================================")
            sb.appendLine(empresa.nomeFantasia.uppercase(Locale.getDefault()))
            if (!empresa.razaoSocial.isNullOrBlank()) sb.appendLine(empresa.razaoSocial)
            if (!empresa.cnpj.isNullOrBlank()) sb.appendLine("CNPJ/CPF: ${empresa.cnpj}")
            if (!empresa.endereco.isNullOrBlank()) sb.appendLine(empresa.endereco)
            if (!empresa.telefone.isNullOrBlank()) sb.appendLine("Tel: ${empresa.telefone}")
            sb.appendLine("================================")
        }
        sb.appendLine("COMPROVANTE DE VENDA")
        sb.appendLine("Venda ID: ${venda.id.takeIf { it.isNotBlank() } ?: "N/A"}")
        sb.appendLine("Data/Hora: ${sdf.format(venda.dataHora?.toDate() ?: Date())}")
        sb.appendLine("Cliente: ${venda.clienteNome ?: "Cliente Padrão"}")
        sb.appendLine("--------------------------------")
        sb.appendLine(String.format(Locale.getDefault(), "%-3s %-18s %10s", "Qtd", "Descricao", "Preco"))
        venda.itens.forEach { item ->
            val nome = item.nomeProduto.take(18).padEnd(18)
            val preco = fMoeda.format(item.totalItem)
            sb.appendLine(String.format(Locale.getDefault(), "%3d %s %10s", item.quantidade, nome, preco))
            if (item.opcoesSelecionadas.isNotEmpty()) {
                val ops = item.opcoesSelecionadas.joinToString { it.nome }
                sb.appendLine("    ($ops)")
            }
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("TOTAL: ${fMoeda.format(venda.totalVenda)}")
        sb.appendLine("Pagamentos:")
        venda.pagamentos.forEach { p ->
            sb.appendLine("  ${p.forma}: ${fMoeda.format(p.valor)}")
        }
        sb.appendLine()
        sb.appendLine("Obrigado pela preferência!")
        sb.appendLine()
        return sb.toString()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared")
    }

    object BluetoothPrinter {
        private val TAG = "BluetoothPrinter"

        @SuppressLint("MissingPermission")
        suspend fun printToBluetooth(ctx: Context, deviceAddress: String, bytes: ByteArray, timeoutMs: Long = 15_000L): Result<Unit> {
            return withContext(Dispatchers.IO) {
                try {
                    val bluetoothManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                        ?: return@withContext Result.failure(IOException("BluetoothManager não disponível"))
                    val adapter = bluetoothManager.adapter ?: return@withContext Result.failure(IOException("Bluetooth não suportado"))

                    if (!adapter.isEnabled) return@withContext Result.failure(IOException("Bluetooth desligado"))

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return@withContext Result.failure(SecurityException("Permissão BLUETOOTH_CONNECT não concedida"))
                    }

                    val device: BluetoothDevice = try {
                        adapter.getRemoteDevice(deviceAddress)
                    } catch (e: IllegalArgumentException) {
                        return@withContext Result.failure(IOException("Endereço de dispositivo inválido: $deviceAddress"))
                    }

                    val sppUuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                    var socket: BluetoothSocket? = null
                    var output: OutputStream? = null

                    withTimeout(timeoutMs) {
                        try {
                            socket = device.createRfcommSocketToServiceRecord(sppUuid)
                            socket?.connect() ?: throw IOException("Falha ao criar socket")
                            if (!socket!!.isConnected) throw IOException("Falha ao conectar socket")

                            output = socket!!.outputStream
                            output!!.write(bytes)
                            output!!.flush()
                        } finally {
                            try { output?.close() } catch (_: Exception) {}
                            try { socket?.close() } catch (_: Exception) {}
                        }
                    }

                    Result.success(Unit)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro imprimir Bluetooth: ${e.message}", e)
                    Result.failure(e)
                }
            }
        }
    }
}