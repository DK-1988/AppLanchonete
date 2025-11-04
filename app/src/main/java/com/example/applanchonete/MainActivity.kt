package com.example.applanchonete

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tipoUsuario = intent.getStringExtra("TIPO_USUARIO")
        Log.i("MainActivity", "Usuário logado com tipo: $tipoUsuario")

        val btnCadastro: Button = findViewById(R.id.btnCadastro)
        val btnPontoDeVendas: Button = findViewById(R.id.btnPontoDeVendas)
        val btnRelatorios: Button = findViewById(R.id.btnRelatorios)
        val btnConfiguracoes: Button = findViewById(R.id.btnConfiguracoes)

        if (tipoUsuario != "Admin") {

            Log.w("MainActivity", "Acesso restrito. Escondendo botões de Admin.")

            btnCadastro.visibility = View.GONE
            btnRelatorios.visibility = View.GONE
            btnConfiguracoes.visibility = View.GONE

        } else {
            Log.i("MainActivity", "Acesso de Administrador concedido.")
            btnCadastro.visibility = View.VISIBLE
            btnRelatorios.visibility = View.VISIBLE
            btnConfiguracoes.visibility = View.VISIBLE
        }
        btnCadastro.setOnClickListener {
            val intent = Intent(this, CadastroMenuActivity::class.java)
            startActivity(intent)
        }

        btnPontoDeVendas.setOnClickListener {
            val intent = Intent(this, VendasActivity::class.java)
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


    }
}