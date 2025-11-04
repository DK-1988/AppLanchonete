package com.example.applanchonete

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

class AdicionarClienteActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private var clienteId: String? = null
    private lateinit var tvTitulo: TextView
    private lateinit var rgTipoCliente: RadioGroup
    private lateinit var rbPessoaFisica: RadioButton
    private lateinit var rbPessoaJuridica: RadioButton
    private lateinit var tilNomeRazaoSocial: TextInputLayout
    private lateinit var tilNomeFantasia: TextInputLayout
    private lateinit var tilCpfCnpj: TextInputLayout
    private lateinit var etNomeRazaoSocial: TextInputEditText
    private lateinit var etNomeFantasia: TextInputEditText
    private lateinit var etCpfCnpj: TextInputEditText
    private lateinit var etTelefone: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etEndereco: TextInputEditText

    private lateinit var btnSalvar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adicionar_cliente)

        db = FirebaseFirestore.getInstance()

        ligarComponentes()

        configurarRadioGroupListener()

        clienteId = intent.getStringExtra("CLIENTE_ID")

        if (clienteId == null) {
            tvTitulo.text = "Novo Cliente"
            btnSalvar.text = "Salvar Cliente"
            atualizarCamposParaTipo(false)
        } else {
            tvTitulo.text = "Editar Cliente"
            btnSalvar.text = "Atualizar Cliente"
            carregarDadosDoCliente(clienteId!!)
        }

        btnSalvar.setOnClickListener {
            validarESalvarCliente()
        }
    }
    private fun ligarComponentes() {
        tvTitulo = findViewById(R.id.tvTituloFormularioCliente)
        rgTipoCliente = findViewById(R.id.rgTipoCliente)
        rbPessoaFisica = findViewById(R.id.rbPessoaFisica)
        rbPessoaJuridica = findViewById(R.id.rbPessoaJuridica)

        tilNomeRazaoSocial = findViewById(R.id.tilNomeRazaoSocial)
        tilNomeFantasia = findViewById(R.id.tilNomeFantasia)
        tilCpfCnpj = findViewById(R.id.tilCpfCnpj)

        etNomeRazaoSocial = findViewById(R.id.etNomeRazaoSocial)
        etNomeFantasia = findViewById(R.id.etNomeFantasia)
        etCpfCnpj = findViewById(R.id.etCpfCnpj)
        etTelefone = findViewById(R.id.etTelefoneCliente)
        etEmail = findViewById(R.id.etEmailCliente)
        etEndereco = findViewById(R.id.etEnderecoCliente)

        btnSalvar = findViewById(R.id.btnSalvarCliente)
    }

    private fun configurarRadioGroupListener() {
        rgTipoCliente.setOnCheckedChangeListener { _, checkedId ->
            atualizarCamposParaTipo(checkedId == R.id.rbPessoaJuridica)
        }
    }

    private fun atualizarCamposParaTipo(isPJ: Boolean) {
        if (isPJ) {
            tilNomeRazaoSocial.hint = "Razão Social"
            tilNomeFantasia.visibility = View.VISIBLE
            tilCpfCnpj.hint = "CNPJ"
        } else {
            tilNomeRazaoSocial.hint = "Nome Completo"
            tilNomeFantasia.visibility = View.GONE
            tilCpfCnpj.hint = "CPF"
        }
    }

    private fun carregarDadosDoCliente(id: String) {
        db.collection("clientes").document(id)
            .get()
            .addOnSuccessListener { document ->
                val cliente = document.toObject(Cliente::class.java)
                if (cliente != null) {
                    preencherFormulario(cliente)
                } else {
                    Toast.makeText(this, "Cliente não encontrado.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar dados.", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun preencherFormulario(cliente: Cliente) {
        if (cliente.tipoCliente == "PJ") {
            rbPessoaJuridica.isChecked = true
        } else {
            rbPessoaFisica.isChecked = true
        }

        etNomeRazaoSocial.setText(cliente.nomeRazaoSocial)
        etNomeFantasia.setText(cliente.nomeFantasia)
        etCpfCnpj.setText(cliente.cpfCnpj)
        etTelefone.setText(cliente.telefone)
        etEmail.setText(cliente.email)
        etEndereco.setText(cliente.endereco)
    }
    private fun validarESalvarCliente() {
        val tipoCliente = if (rbPessoaJuridica.isChecked) "PJ" else "PF"
        val nomeRazao = etNomeRazaoSocial.text.toString().trim()
        val nomeFantasia = etNomeFantasia.text.toString().trim()
        val cpfCnpj = etCpfCnpj.text.toString().trim()
        val telefone = etTelefone.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val endereco = etEndereco.text.toString().trim()

        if (nomeRazao.isEmpty()) {
            etNomeRazaoSocial.error = "Campo obrigatório"
            return
        }
        if (cpfCnpj.isEmpty()) {
            etCpfCnpj.error = "Campo obrigatório"
            return
        }
        if (telefone.isEmpty()) {
            etTelefone.error = "Campo obrigatório"
            return
        }
        if (tipoCliente == "PJ" && nomeFantasia.isEmpty()) {
            etNomeFantasia.error = "Campo obrigatório para PJ"
            return
        }

        val cliente = Cliente(
            id = clienteId ?: "",
            tipoCliente = tipoCliente,
            nomeRazaoSocial = nomeRazao,
            nomeFantasia = if (tipoCliente == "PJ") nomeFantasia else "",
            cpfCnpj = cpfCnpj,
            telefone = telefone,
            email = email,
            endereco = endereco
        )

        if (clienteId == null) {
            salvarNovoCliente(cliente)
        } else {
            atualizarCliente(cliente)
        }
    }

    private fun salvarNovoCliente(cliente: Cliente) {
        db.collection("clientes")
            .add(cliente)
            .addOnSuccessListener {
                Toast.makeText(this, "Cliente salvo com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun atualizarCliente(cliente: Cliente) {
        db.collection("clientes").document(cliente.id)
            .set(cliente)
            .addOnSuccessListener {
                Toast.makeText(this, "Cliente atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao atualizar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}