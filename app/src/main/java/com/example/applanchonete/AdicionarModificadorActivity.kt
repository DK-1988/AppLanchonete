package com.example.applanchonete

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class AdicionarModificadorActivity : AppCompatActivity(), ModificadorOpcaoAdapter.OnOpcaoRemoveListener {

    private val TAG = "AddModificadorActivity"
    private lateinit var db: FirebaseFirestore
    private var modificadorGrupoId: String? = null
    private var isEditMode = false
    private lateinit var tvTitulo: TextView
    private lateinit var etNomeGrupo: TextInputEditText
    private lateinit var rgTipoSelecao: RadioGroup
    private lateinit var rbSelecaoUnica: RadioButton
    private lateinit var rbSelecaoMultipla: RadioButton
    private lateinit var cbObrigatorio: CheckBox
    private lateinit var btnSalvarGrupo: Button
    private lateinit var etNomeOpcao: TextInputEditText
    private lateinit var etPrecoOpcao: TextInputEditText
    private lateinit var btnAdicionarOpcao: Button
    private lateinit var rvOpcoes: RecyclerView
    private lateinit var adapterOpcoes: ModificadorOpcaoAdapter
    private val listaOpcoesTemporaria = mutableListOf<ModificadorOpcao>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adicionar_modificador)

        db = FirebaseFirestore.getInstance()

        ligarComponentesUI()
        setupRecyclerViewOpcoes()
        configurarListeners()

        modificadorGrupoId = intent.getStringExtra("MODIFICADOR_GRUPO_ID")

        if (modificadorGrupoId == null) {
            isEditMode = false
            tvTitulo.text = "Novo Grupo de Modificadores"
            btnSalvarGrupo.text = "Salvar Grupo"
        } else {
            isEditMode = true
            tvTitulo.text = "Editar Grupo de Modificadores"
            btnSalvarGrupo.text = "Atualizar Grupo"
            carregarDadosDoGrupo(modificadorGrupoId!!)
        }
    }

    private fun ligarComponentesUI() {
        tvTitulo = findViewById(R.id.tvTituloFormularioModificador)
        etNomeGrupo = findViewById(R.id.etNomeGrupoModificador)
        rgTipoSelecao = findViewById(R.id.rgTipoSelecaoModificador)
        rbSelecaoUnica = findViewById(R.id.rbSelecaoUnica)
        rbSelecaoMultipla = findViewById(R.id.rbSelecaoMultipla)
        cbObrigatorio = findViewById(R.id.cbModificadorObrigatorio)
        btnSalvarGrupo = findViewById(R.id.btnSalvarModificadorGrupo)
        etNomeOpcao = findViewById(R.id.etNomeOpcao)
        etPrecoOpcao = findViewById(R.id.etPrecoOpcao)
        btnAdicionarOpcao = findViewById(R.id.btnAdicionarOpcao)
        rvOpcoes = findViewById(R.id.rvOpcoesModificador)
    }

    private fun setupRecyclerViewOpcoes() {
        adapterOpcoes = ModificadorOpcaoAdapter(listaOpcoesTemporaria, this)
        rvOpcoes.adapter = adapterOpcoes
        rvOpcoes.layoutManager = LinearLayoutManager(this)
    }

    private fun configurarListeners() {
        btnAdicionarOpcao.setOnClickListener {
            adicionarOpcaoNaLista()
        }

        btnSalvarGrupo.setOnClickListener {
            validarESalvarGrupo()
        }
    }

    private fun carregarDadosDoGrupo(id: String) {
        db.collection("modificadores").document(id)
            .get()
            .addOnSuccessListener { document ->
                val grupo = document.toObject(ModificadorGrupo::class.java)
                if (grupo != null) {
                    preencherFormulario(grupo)
                } else {
                    Toast.makeText(this, "Grupo não encontrado.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar grupo: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun preencherFormulario(grupo: ModificadorGrupo) {
        etNomeGrupo.setText(grupo.nome)
        cbObrigatorio.isChecked = grupo.obrigatorio

        if (grupo.tipoSelecao == "UNICA") {
            rbSelecaoUnica.isChecked = true
        } else {
            rbSelecaoMultipla.isChecked = true
        }

        listaOpcoesTemporaria.clear()
        listaOpcoesTemporaria.addAll(grupo.opcoes)

        adapterOpcoes.atualizarLista(listaOpcoesTemporaria)
    }

    private fun adicionarOpcaoNaLista() {
        val nomeOpcao = etNomeOpcao.text.toString().trim()
        val precoStr = etPrecoOpcao.text.toString().trim()

        if (nomeOpcao.isEmpty()) {
            etNomeOpcao.error = "Nome é obrigatório"
            etNomeOpcao.requestFocus()
            return
        }

        val precoOpcao = if (precoStr.isEmpty()) 0.0 else precoStr.toDoubleOrNull()
        if (precoOpcao == null) {
            etPrecoOpcao.error = "Valor inválido"
            etPrecoOpcao.requestFocus()
            return
        }

        val novaOpcao = ModificadorOpcao(nome = nomeOpcao, precoAdicional = precoOpcao)
        listaOpcoesTemporaria.add(novaOpcao)

        adapterOpcoes.atualizarLista(listaOpcoesTemporaria)

        etNomeOpcao.text?.clear()
        etPrecoOpcao.text?.clear()
        etNomeOpcao.requestFocus()
    }

    override fun onRemoveOpcaoClicked(position: Int) {
        if (position >= 0 && position < listaOpcoesTemporaria.size) {
            listaOpcoesTemporaria.removeAt(position)
            adapterOpcoes.atualizarLista(listaOpcoesTemporaria) // Atualiza a UI
        }
    }

    private fun validarESalvarGrupo() {
        val nomeGrupo = etNomeGrupo.text.toString().trim()

        if (nomeGrupo.isEmpty()) {
            etNomeGrupo.error = "Nome do grupo é obrigatório"
            etNomeGrupo.requestFocus()
            return
        }

        if (listaOpcoesTemporaria.isEmpty()) {
            Toast.makeText(this, "Adicione pelo menos uma opção ao grupo.", Toast.LENGTH_SHORT).show()
            etNomeOpcao.requestFocus()
            return
        }

        val tipoSelecao = if (rbSelecaoUnica.isChecked) "UNICA" else "MULTIPLA"
        val obrigatorio = cbObrigatorio.isChecked

        val grupo = ModificadorGrupo(
            id = modificadorGrupoId ?: "",
            nome = nomeGrupo,
            tipoSelecao = tipoSelecao,
            obrigatorio = obrigatorio,
            opcoes = listaOpcoesTemporaria.toList()
        )

        if (isEditMode) {
            atualizarGrupo(grupo)
        } else {
            salvarNovoGrupo(grupo)
        }
    }

    private fun salvarNovoGrupo(grupo: ModificadorGrupo) {
        db.collection("modificadores")
            .add(grupo)
            .addOnSuccessListener {
                Toast.makeText(this, "Grupo salvo com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao salvar grupo", e)
                Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun atualizarGrupo(grupo: ModificadorGrupo) {
        db.collection("modificadores").document(grupo.id)
            .set(grupo) // .set() sobrescreve o documento
            .addOnSuccessListener {
                Toast.makeText(this, "Grupo atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao atualizar grupo", e)
                Toast.makeText(this, "Erro ao atualizar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}