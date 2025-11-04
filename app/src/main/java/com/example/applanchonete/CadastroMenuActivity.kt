package com.example.applanchonete

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

class CadastroMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_menu)

        val btnMenuProdutos: Button = findViewById(R.id.btnMenuProdutos)
        val btnMenuClientes: Button = findViewById(R.id.btnMenuClientes)
        val btnMenuSetores: Button = findViewById(R.id.btnMenuSetores)
        val btnMenuModificadores: Button = findViewById(R.id.btnMenuModificadores)

        btnMenuProdutos.setOnClickListener {
            val intent = Intent(this, ProdutosActivity::class.java)
            startActivity(intent)
        }

        btnMenuClientes.setOnClickListener {
            val intent = Intent(this, ClientesActivity::class.java)
            startActivity(intent)
        }

        btnMenuSetores.setOnClickListener {
            val intent = Intent(this, SetoresActivity::class.java)
            startActivity(intent)
        }

        btnMenuModificadores.setOnClickListener {
            val intent = Intent(this, ModificadoresActivity::class.java)
            startActivity(intent)
        }
    }
}