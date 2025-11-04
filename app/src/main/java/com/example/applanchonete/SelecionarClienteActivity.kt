package com.example.applanchonete

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast // Import correto
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SelecionarClienteActivity : AppCompatActivity(), SelecionarClienteAdapter.OnClienteSelecionadoListener {

    private val TAG = "SelecionarCliente"
    private lateinit var db: FirebaseFirestore

    private lateinit var rvClientes: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var adapterCliente: SelecionarClienteAdapter
    private val listaClientesCompleta = mutableListOf<Cliente>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selecionar_cliente)

        db = FirebaseFirestore.getInstance()

        rvClientes = findViewById(R.id.rvSelecionarCliente)
        searchView = findViewById(R.id.searchViewCliente)

        setupRecyclerView()
        setupSearchView()
        carregarClientesDoFirebase()
    }

    private fun setupRecyclerView() {
        adapterCliente = SelecionarClienteAdapter(listaClientesCompleta, this)
        rvClientes.adapter = adapterCliente
        rvClientes.layoutManager = LinearLayoutManager(this)
    }

    private fun carregarClientesDoFirebase() {
        db.collection("clientes")
            .orderBy("nomeRazaoSocial", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Toast.makeText(this, "Nenhum cliente cadastrado.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                listaClientesCompleta.clear()
                snapshot.documents.mapNotNullTo(listaClientesCompleta) { doc ->
                    doc.toObject(Cliente::class.java)?.apply { id = doc.id }
                }
                adapterCliente.atualizarListaCompleta(listaClientesCompleta)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao carregar clientes", e)
                Toast.makeText(this, "Erro ao carregar clientes.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                adapterCliente.filter.filter(query)
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                adapterCliente.filter.filter(newText)
                return true
            }
        })
    }

    override fun onClienteSelecionado(cliente: Cliente) {
        val resultIntent = Intent()
        resultIntent.putExtra("CLIENTE_ID_RESULTADO", cliente.id)
        resultIntent.putExtra("CLIENTE_NOME_RESULTADO", cliente.nomeRazaoSocial)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}