package com.example.applanchonete

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.ktx.auth // Import para Auth
import com.google.firebase.ktx.Firebase // Import para Firebase

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tipoUsuario = intent.getStringExtra("TIPO_USUARIO")
        Log.i("MainActivity", "Usuário logado com tipo: $tipoUsuario")

        val btnPontoDeVendas: Button = findViewById(R.id.btnPontoDeVendas)
        val btnCadastro: Button = findViewById(R.id.btnCadastro)
        val btnRelatorios: Button = findViewById(R.id.btnRelatorios)
        val btnConfiguracoes: Button = findViewById(R.id.btnConfiguracoes)
        val btnSair: Button = findViewById(R.id.btnSair) // NOVO

        if (tipoUsuario != "Admin") {
            btnCadastro.visibility = View.GONE
            btnRelatorios.visibility = View.GONE
            btnConfiguracoes.visibility = View.GONE
        } else {
            btnCadastro.visibility = View.VISIBLE
            btnRelatorios.visibility = View.VISIBLE
            btnConfiguracoes.visibility = View.VISIBLE
        }


        btnPontoDeVendas.setOnClickListener {
            val intent = Intent(this, VendasActivity::class.java)
            startActivity(intent)
        }

        btnCadastro.setOnClickListener {
            val intent = Intent(this, CadastroMenuActivity::class.java)
            startActivity(intent)
        }

        btnRelatorios.setOnClickListener {
            val intent = Intent(this, RelatoriosActivity::class.java)
            startActivity(intent)
        }

        btnConfiguracoes.setOnClickListener {
            val intent = Intent(this, ConfiguracoesMenuActivity::class.java)
            startActivity(intent)
        }
        
        btnSair.setOnClickListener {
            Firebase.auth.signOut()

            Toast.makeText(this, "Deslogado com sucesso.", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LoginActivity::class.java)

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
        }
    }
}