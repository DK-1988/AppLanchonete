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

// --- NOVO (Passo 154): Implementa a interface do adapter de opções ---
class AdicionarModificadorActivity : AppCompatActivity(), ModificadorOpcaoAdapter.OnOpcaoRemoveListener {

    private val TAG = "AddModificadorActivity"

    // --- Firebase ---
    private lateinit var db: FirebaseFirestore

    // --- Lógica de Edição ---
    private var modificadorGrupoId: String? = null
    private var isEditMode = false

    // --- Componentes de UI ---
    private lateinit var tvTitulo: TextView
    private lateinit var etNomeGrupo: TextInputEditText
    private lateinit var rgTipoSelecao: RadioGroup
    private lateinit var rbSelecaoUnica: RadioButton
    private lateinit var rbSelecaoMultipla: RadioButton
    private lateinit var cbObrigatorio: CheckBox
    private lateinit var btnSalvarGrupo: Button

    // --- Componentes da Lista de Opções Interna ---
    private lateinit var etNomeOpcao: TextInputEditText
    private lateinit var etPrecoOpcao: TextInputEditText
    private lateinit var btnAdicionarOpcao: Button
    private lateinit var rvOpcoes: RecyclerView
    private lateinit var adapterOpcoes: ModificadorOpcaoAdapter

    // Lista temporária para as opções (a "fonte da verdade")
    private val listaOpcoesTemporaria = mutableListOf<ModificadorOpcao>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adicionar_modificador)

        db = FirebaseFirestore.getInstance()

        // Ligar todos os componentes de UI
        ligarComponentesUI()

        // Configurar a RecyclerView interna
        setupRecyclerViewOpcoes()

        // Configurar os cliques dos botões
        configurarListeners()

        // Checar se estamos em modo Adicionar ou Editar
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

    /**
     * Liga todas as Views às variáveis da classe
     */
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

    /**
     * Configura a RecyclerView interna para as opções
     */
    private fun setupRecyclerViewOpcoes() {
        // Inicializa o adapter (Passo 152), passando 'this' como listener
        adapterOpcoes = ModificadorOpcaoAdapter(listaOpcoesTemporaria, this)
        rvOpcoes.adapter = adapterOpcoes
        rvOpcoes.layoutManager = LinearLayoutManager(this)
    }

    /**
     * Configura os cliques do botão "Add Opção" e "Salvar Grupo"
     */
    private fun configurarListeners() {
        // Clique para adicionar uma opção à lista temporária
        btnAdicionarOpcao.setOnClickListener {
            adicionarOpcaoNaLista()
        }

        // Clique para salvar o grupo inteiro no Firebase
        btnSalvarGrupo.setOnClickListener {
            validarESalvarGrupo()
        }
    }

    /**
     * (Modo Editar) Busca os dados do grupo no Firestore
     */
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

    /**
     * (Modo Editar) Preenche o formulário com os dados carregados
     */
    private fun preencherFormulario(grupo: ModificadorGrupo) {
        etNomeGrupo.setText(grupo.nome)
        cbObrigatorio.isChecked = grupo.obrigatorio

        if (grupo.tipoSelecao == "UNICA") {
            rbSelecaoUnica.isChecked = true
        } else {
            rbSelecaoMultipla.isChecked = true
        }

        // Preenche a lista temporária com as opções salvas
        listaOpcoesTemporaria.clear()
        listaOpcoesTemporaria.addAll(grupo.opcoes)

        // Atualiza o adapter da lista interna
        adapterOpcoes.atualizarLista(listaOpcoesTemporaria)
    }

    /**
     * Pega os dados dos campos "Nome da Opção" e "Preço", valida
     * e adiciona na 'listaOpcoesTemporaria'.
     */
    private fun adicionarOpcaoNaLista() {
        val nomeOpcao = etNomeOpcao.text.toString().trim()
        val precoStr = etPrecoOpcao.text.toString().trim()

        // Validação
        if (nomeOpcao.isEmpty()) {
            etNomeOpcao.error = "Nome é obrigatório"
            etNomeOpcao.requestFocus()
            return
        }

        // Preço é opcional (default 0.0)
        val precoOpcao = if (precoStr.isEmpty()) 0.0 else precoStr.toDoubleOrNull()
        if (precoOpcao == null) {
            etPrecoOpcao.error = "Valor inválido"
            etPrecoOpcao.requestFocus()
            return
        }

        // Cria o objeto e adiciona na lista
        val novaOpcao = ModificadorOpcao(nome = nomeOpcao, precoAdicional = precoOpcao)
        listaOpcoesTemporaria.add(novaOpcao)

        // Notifica o adapter
        adapterOpcoes.atualizarLista(listaOpcoesTemporaria)

        // Limpa os campos para a próxima adição
        etNomeOpcao.text?.clear()
        etPrecoOpcao.text?.clear()
        etNomeOpcao.requestFocus()
    }

    /**
     * --- CALLBACK (Passo 154) ---
     * Esta função é chamada pelo ModificadorOpcaoAdapter quando
     * o usuário clica no "X" (remover) de um item.
     */
    override fun onRemoveOpcaoClicked(position: Int) {
        if (position >= 0 && position < listaOpcoesTemporaria.size) {
            listaOpcoesTemporaria.removeAt(position)
            adapterOpcoes.atualizarLista(listaOpcoesTemporaria) // Atualiza a UI
        }
    }

    /**
     * Valida os campos principais, constrói o objeto ModificadorGrupo
     * e decide se deve criar ou atualizar no Firebase.
     */
    private fun validarESalvarGrupo() {
        val nomeGrupo = etNomeGrupo.text.toString().trim()

        // 1. Validação do Nome do Grupo
        if (nomeGrupo.isEmpty()) {
            etNomeGrupo.error = "Nome do grupo é obrigatório"
            etNomeGrupo.requestFocus()
            return
        }

        // 2. Validação da Lista de Opções
        // (Opcional: você pode exigir que tenha pelo menos 1 opção)
        if (listaOpcoesTemporaria.isEmpty()) {
            Toast.makeText(this, "Adicione pelo menos uma opção ao grupo.", Toast.LENGTH_SHORT).show()
            etNomeOpcao.requestFocus()
            return
        }

        // 3. Pega os dados dos RadioButtons e CheckBox
        val tipoSelecao = if (rbSelecaoUnica.isChecked) "UNICA" else "MULTIPLA"
        val obrigatorio = cbObrigatorio.isChecked

        // 4. Constrói o objeto final
        val grupo = ModificadorGrupo(
            id = modificadorGrupoId ?: "", // ID (vazio se for novo)
            nome = nomeGrupo,
            tipoSelecao = tipoSelecao,
            obrigatorio = obrigatorio,
            opcoes = listaOpcoesTemporaria.toList() // Salva a lista de opções
        )

        // 5. Decide se salva ou atualiza
        if (isEditMode) {
            atualizarGrupo(grupo)
        } else {
            salvarNovoGrupo(grupo)
        }
    }

    /**
     * (Modo Adicionar) Salva o novo grupo no Firestore
     */
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

    /**
     * (Modo Editar) Atualiza o grupo existente no Firestore
     */
    private fun atualizarGrupo(grupo: ModificadorGrupo) {
        // O grupo.id já contém o ID correto que carregamos
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