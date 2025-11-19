package com.example.applanchonete

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import androidx.appcompat.app.AlertDialog

class AdicionarProdutoActivity : AppCompatActivity() {

    private val TAG = "AddProdutoActivity"
    private lateinit var db: FirebaseFirestore
    private var produtoId: String? = null
    private lateinit var etNomeProduto: TextInputEditText
    private lateinit var etPrecoCusto: TextInputEditText
    private lateinit var etMargemLucro: TextInputEditText
    private lateinit var etPrecoVenda: TextInputEditText
    private lateinit var etQuantidadeEstoque: TextInputEditText
    private lateinit var btnSalvarProduto: Button
    private lateinit var tvTituloFormulario: TextView
    private lateinit var spinnerSetor: Spinner
    private lateinit var tvModificadoresSelecionados: TextView
    private lateinit var btnSelecionarModificadores: Button
    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private val nomesSetores = mutableListOf<String>()
    private val listaTodosModificadores = mutableListOf<ModificadorGrupo>()
    private val listaModificadoresSelecionadosIds = mutableListOf<String>()
    private var isCalculating = false
    private var campoEmFoco: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adicionar_produto)

        db = FirebaseFirestore.getInstance()

        ligarComponentesUI()
        setupSetorSpinner()
        configurarListenersDeCalculo()

        btnSelecionarModificadores.setOnClickListener {
            mostrarDialogoModificadores()
        }

        carregarDadosExternos()

        btnSalvarProduto.setOnClickListener {
            validarECadastrarProduto()
        }
    }

    private fun ligarComponentesUI() {
        etNomeProduto = findViewById(R.id.etNomeProduto)
        etPrecoCusto = findViewById(R.id.etPrecoCusto)
        etMargemLucro = findViewById(R.id.etMargemLucro)
        etPrecoVenda = findViewById(R.id.etPrecoVenda)
        etQuantidadeEstoque = findViewById(R.id.etQuantidadeEstoque)
        btnSalvarProduto = findViewById(R.id.btnSalvarProduto)
        tvTituloFormulario = findViewById(R.id.tvTituloFormulario)
        spinnerSetor = findViewById(R.id.spinnerSetor)
        tvModificadoresSelecionados = findViewById(R.id.tvModificadoresSelecionados)
        btnSelecionarModificadores = findViewById(R.id.btnSelecionarModificadores)
    }

    private fun setupSetorSpinner() {
        nomesSetores.add("Selecione um setor...")
        spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nomesSetores)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSetor.adapter = spinnerAdapter
    }

    private fun configurarListenersDeCalculo() {
        val focusListener = View.OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                campoEmFoco = view
            }
        }
        etMargemLucro.onFocusChangeListener = focusListener
        etPrecoVenda.onFocusChangeListener = focusListener

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calcularCampos()
            }
        }
        etPrecoCusto.addTextChangedListener(textWatcher)
        etMargemLucro.addTextChangedListener(textWatcher)
        etPrecoVenda.addTextChangedListener(textWatcher)
    }

    private fun calcularCampos() {
        if (isCalculating) return
        isCalculating = true

        val custo = etPrecoCusto.text.toString().toDoubleOrNull()

        if (custo == null || custo <= 0) {
            if(campoEmFoco != etPrecoCusto) {
                etMargemLucro.setText("")
                etPrecoVenda.setText("")
            }
            isCalculating = false
            return
        }

        if (campoEmFoco == etMargemLucro) {
            val margemPct = etMargemLucro.text.toString().toDoubleOrNull()
            if (margemPct != null) {
                val precoVenda = custo * (1 + (margemPct / 100))
                etPrecoVenda.setText(String.format(Locale.US, "%.2f", precoVenda))
            } else {
                etPrecoVenda.setText("")
            }
        }
        else if (campoEmFoco == etPrecoVenda) {
            val precoVenda = etPrecoVenda.text.toString().toDoubleOrNull()
            if (precoVenda != null && precoVenda >= custo) {
                val margemPct = ((precoVenda - custo) / custo) * 100
                etMargemLucro.setText(String.format(Locale.US, "%.2f", margemPct))
            } else {
                etMargemLucro.setText("")
            }
        }
        else if (campoEmFoco == etPrecoCusto) {
            val margemPct = etMargemLucro.text.toString().toDoubleOrNull()
            if (margemPct != null) {
                val precoVenda = custo * (1 + (margemPct / 100))
                etPrecoVenda.setText(String.format(Locale.US, "%.2f", precoVenda))
            } else {
                val precoVenda = etPrecoVenda.text.toString().toDoubleOrNull()
                if (precoVenda != null && precoVenda >= custo) {
                    val margemPctCalc = ((precoVenda - custo) / custo) * 100
                    etMargemLucro.setText(String.format(Locale.US, "%.2f", margemPctCalc))
                }
            }
        }
        isCalculating = false
    }

    private fun carregarDadosExternos() {
        db.collection("setores").orderBy("nome").get()
            .addOnSuccessListener { snapshotSetores ->
                nomesSetores.clear(); nomesSetores.add("Selecione um setor...")
                snapshotSetores.documents.mapNotNullTo(nomesSetores) { doc -> doc.toObject(Setor::class.java)?.nome }
                spinnerAdapter.notifyDataSetChanged()

                db.collection("modificadores").orderBy("nome").get()
                    .addOnSuccessListener { snapshotModificadores ->
                        listaTodosModificadores.clear()
                        snapshotModificadores.documents.mapNotNullTo(listaTodosModificadores) { doc ->
                            doc.toObject(ModificadorGrupo::class.java)?.apply { id = doc.id }
                        }
                        verificarModoEdicao()
                    }
                    .addOnFailureListener { e -> Log.w(TAG, "Erro ao carregar modificadores", e); verificarModoEdicao() }
            }
            .addOnFailureListener { e -> Log.w(TAG, "Erro ao carregar setores", e); verificarModoEdicao() }
    }

    private fun verificarModoEdicao() {
        produtoId = intent.getStringExtra("PRODUTO_ID")
        if (produtoId == null) {
            tvTituloFormulario.text = "Novo Produto"
            btnSalvarProduto.text = "Salvar Produto"
        } else {
            tvTituloFormulario.text = "Editar Produto"
            btnSalvarProduto.text = "Atualizar Produto"
            carregarDadosDoProduto(produtoId!!)
        }
    }

    private fun carregarDadosDoProduto(id: String) {
        db.collection("produtos").document(id).get()
            .addOnSuccessListener { document ->
                val produto = document.toObject(Produto::class.java)
                if (produto != null) { preencherFormulario(produto) }
                else { Toast.makeText(this, "Produto não encontrado.", Toast.LENGTH_SHORT).show(); finish() }
            }
            .addOnFailureListener { e -> Toast.makeText(this, "Erro ao carregar: ${e.message}", Toast.LENGTH_SHORT).show(); finish() }
    }

    private fun preencherFormulario(produto: Produto) {
        isCalculating = true
        etNomeProduto.setText(produto.nome)
        etPrecoCusto.setText(String.format(Locale.US, "%.2f", produto.precoCusto))
        etPrecoVenda.setText(String.format(Locale.US, "%.2f", produto.precoVenda))
        etQuantidadeEstoque.setText(produto.quantidadeEstoque.toString())

        val margemPct = produto.margemLucro * 100
        etMargemLucro.setText(String.format(Locale.US, "%.2f", margemPct))

        selecionarSetorNoSpinner(produto.setor)

        listaModificadoresSelecionadosIds.clear()
        listaModificadoresSelecionadosIds.addAll(produto.modificadorGrupoIds)
        atualizarTextoModificadores()

        isCalculating = false
    }

    private fun selecionarSetorNoSpinner(nomeDoSetor: String) {
        for (i in 0 until spinnerAdapter.count) {
            if (spinnerAdapter.getItem(i) == nomeDoSetor) { spinnerSetor.setSelection(i); return }
        }
    }

    private fun mostrarDialogoModificadores() {
        if (listaTodosModificadores.isEmpty()) {
            Toast.makeText(this, "Nenhum grupo de modificador cadastrado.", Toast.LENGTH_SHORT).show()
            return
        }

        val nomesArray = listaTodosModificadores.map { it.nome }.toTypedArray()
        val checkedArray = listaTodosModificadores.map {
            listaModificadoresSelecionadosIds.contains(it.id)
        }.toBooleanArray()

        val novosIdsSelecionados = mutableListOf<String>()
        novosIdsSelecionados.addAll(listaModificadoresSelecionadosIds)

        AlertDialog.Builder(this)
            .setTitle("Selecionar Grupos")
            .setMultiChoiceItems(nomesArray, checkedArray) { dialog, which, isChecked ->
                val idSelecionado = listaTodosModificadores[which].id
                if (isChecked) {
                    novosIdsSelecionados.add(idSelecionado)
                } else {
                    novosIdsSelecionados.remove(idSelecionado)
                }
            }
            .setPositiveButton("OK") { dialog, _ ->
                listaModificadoresSelecionadosIds.clear()
                listaModificadoresSelecionadosIds.addAll(novosIdsSelecionados)
                atualizarTextoModificadores()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun atualizarTextoModificadores() {
        if (listaModificadoresSelecionadosIds.isEmpty()) {
            tvModificadoresSelecionados.text = "Nenhum grupo selecionado"
            return
        }
        val nomesSelecionados = listaTodosModificadores
            .filter { listaModificadoresSelecionadosIds.contains(it.id) }
            .joinToString { it.nome }
        tvModificadoresSelecionados.text = nomesSelecionados
    }

    private fun validarECadastrarProduto() {
        val nome = etNomeProduto.text.toString().trim()
        val strPrecoCusto = etPrecoCusto.text.toString().trim()
        val strMargemLucro = etMargemLucro.text.toString().trim()
        val strPrecoVenda = etPrecoVenda.text.toString().trim()
        val strQuantidade = etQuantidadeEstoque.text.toString().trim()

        var setor = ""; if (spinnerSetor.selectedItemPosition > 0) { setor = spinnerSetor.selectedItem.toString() }
        else { Toast.makeText(this, "Selecione um setor.", Toast.LENGTH_SHORT).show(); return }

        if (nome.isEmpty() || strPrecoCusto.isEmpty() || strMargemLucro.isEmpty() || strPrecoVenda.isEmpty() || strQuantidade.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show()
            return
        }

        val precoCusto = strPrecoCusto.toDoubleOrNull()
        val margemPct = strMargemLucro.toDoubleOrNull()
        val precoVenda = strPrecoVenda.toDoubleOrNull()
        val quantidade = strQuantidade.toIntOrNull()

        if (precoCusto == null) { etPrecoCusto.error = "Inválido"; return }
        if (margemPct == null) { etMargemLucro.error = "Inválido"; return }
        if (precoVenda == null) { etPrecoVenda.error = "Inválido"; return }
        if (quantidade == null || quantidade < 0) { etQuantidadeEstoque.error = "Inválido"; return }

        val margemDecimal = margemPct / 100.0

        val produto = Produto(
            id = produtoId ?: "",
            nome = nome,
            setor = setor,
            precoCusto = precoCusto,
            margemLucro = margemDecimal,
            precoVenda = precoVenda,
            quantidadeEstoque = quantidade,
            modificadorGrupoIds = listaModificadoresSelecionadosIds.toList()
        )

        if (produtoId == null) { salvarNovoProduto(produto) }
        else { atualizarProduto(produto) }
    }

    private fun salvarNovoProduto(produto: Produto) {
        db.collection("produtos").add(produto)
            .addOnSuccessListener { Toast.makeText(this, "Salvo!", Toast.LENGTH_SHORT).show(); finish() }
            .addOnFailureListener { e -> Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show() }
    }
    private fun atualizarProduto(produto: Produto) {
        db.collection("produtos").document(produto.id).set(produto)
            .addOnSuccessListener { Toast.makeText(this, "Atualizado!", Toast.LENGTH_SHORT).show(); finish() }
            .addOnFailureListener { e -> Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show() }
    }
}