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
    private lateinit var db: FirebaseFirestore
    private lateinit var rvModificadores: RecyclerView
    private lateinit var fabAdicionar: FloatingActionButton
    private lateinit var adapter: ModificadorGrupoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modificadores)

        db = FirebaseFirestore.getInstance()

        rvModificadores = findViewById(R.id.recyclerViewModificadores)
        fabAdicionar = findViewById(R.id.fabAdicionarModificador)

        setupRecyclerView()

        carregarGruposDoFirebase()

        fabAdicionar.setOnClickListener {
            val intent = Intent(this, AdicionarModificadorActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        adapter = ModificadorGrupoAdapter(this, emptyList())

        rvModificadores.adapter = adapter

        rvModificadores.layoutManager = LinearLayoutManager(this)
    }

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
                        val grupo = document.toObject(ModificadorGrupo::class.java)
                        if (grupo != null) {
                            grupo.id = document.id
                            novaLista.add(grupo)
                        }
                    }
                    adapter.atualizarLista(novaLista)
                }
            }
    }
}