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

class ProdutosActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    private lateinit var recyclerViewProdutos: RecyclerView

    private lateinit var adapter: ProdutoAdapter

    private val listaProdutos = mutableListOf<Produto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_produtos)

        db = FirebaseFirestore.getInstance()

        recyclerViewProdutos = findViewById(R.id.recyclerViewProdutos)
        val fabAdicionarProduto: FloatingActionButton = findViewById(R.id.fabAdicionarProduto)

        setupRecyclerView()

        carregarProdutosDoFirebase()

        fabAdicionarProduto.setOnClickListener {
            val intent = Intent(this, AdicionarProdutoActivity::class.java)
            startActivity(intent)
        }
    }
    private fun setupRecyclerView() {
        adapter = ProdutoAdapter(this, listaProdutos)
        recyclerViewProdutos.adapter = adapter
        recyclerViewProdutos.layoutManager = LinearLayoutManager(this)
    }
    private fun carregarProdutosDoFirebase() {
        db.collection("produtos")
            .orderBy("nome", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Log.w("ProdutosActivity", "Erro ao carregar produtos.", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    listaProdutos.clear()
                    for (document in snapshot.documents) {
                        val produto = document.toObject(Produto::class.java)
                        if (produto != null) {
                            produto.id = document.id
                            listaProdutos.add(produto)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }
}