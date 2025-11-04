package com.example.applanchonete

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ModificadoresActivity : AppCompatActivity() {

    private val TAG = "ModificadoresActivity"

    // --- Firebase ---
    private lateinit var db: FirebaseFirestore

    // --- Componentes de UI ---
    private lateinit var rvModificadores: RecyclerView
    private lateinit var fabAdicionar: FloatingActionButton

    // --- Adapter ---
    private lateinit var adapter: ModificadorGrupoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modificadores)

        // Inicializa o Firestore
        db = FirebaseFirestore.getInstance()

        // Ligar componentes de UI
        rvModificadores = findViewById(R.id.recyclerViewModificadores)
        fabAdicionar = findViewById(R.id.fabAdicionarModificador)

        // Configurar a RecyclerView
        setupRecyclerView()

        // Carregar os dados do Firebase
        carregarGruposDoFirebase()

        // Configurar o clique do botão "+"
        fabAdicionar.setOnClickListener {
            // Abre o formulário para criar um NOVO grupo
            val intent = Intent(this, AdicionarModificadorActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * --- Documentação: setupRecyclerView ---
     * Prepara a RecyclerView, inicializando o Adapter e o LayoutManager.
     */
    private fun setupRecyclerView() {
        // Inicializa o adapter (que criamos no Passo 151)
        // Passamos 'this' (Context) e uma lista vazia
        adapter = ModificadorGrupoAdapter(this, emptyList())

        // Define o adapter na RecyclerView
        rvModificadores.adapter = adapter

        // Define a organização (lista vertical)
        rvModificadores.layoutManager = LinearLayoutManager(this)
    }

    /**
     * --- Documentação: carregarGruposDoFirebase ---
     * "Escuta" a coleção "modificadores" em tempo real.
     * O SnapshotListener garante que a lista se atualize
     * automaticamente se um grupo for adicionado, editado ou excluído.
     */
    private fun carregarGruposDoFirebase() {
        db.collection("modificadores")
            .orderBy("nome", Query.Direction.ASCENDING) // Ordena por nome
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Log.w(TAG, "Erro ao carregar modificadores.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val novaLista = mutableListOf<ModificadorGrupo>()

                    for (document in snapshot.documents) {
                        // Converte o documento para o objeto ModificadorGrupo
                        val grupo = document.toObject(ModificadorGrupo::class.java)
                        if (grupo != null) {
                            // Define o ID do objeto
                            grupo.id = document.id
                            novaLista.add(grupo)
                        }
                    }
                    // Envia a lista atualizada para o adapter
                    adapter.atualizarLista(novaLista)
                }
            }
    }
}