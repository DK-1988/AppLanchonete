package com.example.applanchonete

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class ConfiguracoesMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracoes_menu)

        val btnMenuEmpresa: Button = findViewById(R.id.btnMenuEmpresaConfig)
        val btnMenuImpressao: Button = findViewById(R.id.btnMenuImpressao)

        btnMenuEmpresa.setOnClickListener {
            val intent = Intent(this, EmpresaActivity::class.java)
            startActivity(intent)
        }
        btnMenuImpressao.setOnClickListener {
            val intent = Intent(this, ImpressaoConfigActivity::class.java)
            startActivity(intent)
        }
    }
}