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

class SetoresActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var rvSetores: RecyclerView
    private lateinit var fabAdicionarSetor: FloatingActionButton
    private lateinit var adapterSetor: SetorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setores)

        db = FirebaseFirestore.getInstance()

        rvSetores = findViewById(R.id.recyclerViewSetores)
        fabAdicionarSetor = findViewById(R.id.fabAdicionarSetor)

        setupRecyclerView()

        carregarSetoresDoFirebase()

        fabAdicionarSetor.setOnClickListener {
            val intent = Intent(this, AdicionarSetorActivity::class.java)
            startActivity(intent)
        }
    }
    private fun setupRecyclerView() {
        adapterSetor = SetorAdapter(this, emptyList())
        rvSetores.adapter = adapterSetor
        rvSetores.layoutManager = LinearLayoutManager(this)
    }

    private fun carregarSetoresDoFirebase() {
        db.collection("setores")
            .orderBy("nome", Query.Direction.ASCENDING) // Ordena por nome
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Log.w("SetoresActivity", "Erro ao carregar setores.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val novaLista = mutableListOf<Setor>()

                    for (document in snapshot.documents) {
                        val setor = document.toObject(Setor::class.java)
                        if (setor != null) {
                            // Define o ID do objeto
                            setor.id = document.id
                            novaLista.add(setor)
                        }
                    }
                    adapterSetor.atualizarLista(novaLista)
                }
            }
    }
}