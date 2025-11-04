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

class ClientesActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    private lateinit var rvClientes: RecyclerView

    private lateinit var adapterCliente: ClienteAdapter

    private lateinit var fabAdicionarCliente: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clientes)

        db = FirebaseFirestore.getInstance()

        rvClientes = findViewById(R.id.recyclerViewClientes)
        fabAdicionarCliente = findViewById(R.id.fabAdicionarCliente)

        setupRecyclerView()

        carregarClientesDoFirebase()

        fabAdicionarCliente.setOnClickListener {
            val intent = Intent(this, AdicionarClienteActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        adapterCliente = ClienteAdapter(this, emptyList())

        rvClientes.adapter = adapterCliente

        rvClientes.layoutManager = LinearLayoutManager(this)
    }

    private fun carregarClientesDoFirebase() {
        db.collection("clientes")
            .orderBy("nomeRazaoSocial", Query.Direction.ASCENDING) // Ordena por nome
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Log.w("ClientesActivity", "Erro ao carregar clientes.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val novaLista = mutableListOf<Cliente>()

                    // Itera sobre cada documento retornado
                    for (document in snapshot.documents) {
                        val cliente = document.toObject(Cliente::class.java)
                        if (cliente != null) {
                            cliente.id = document.id
                            novaLista.add(cliente)
                        }
                    }
                    adapterCliente.atualizarLista(novaLista)
                }
            }
    }
}