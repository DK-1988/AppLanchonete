package com.example.applanchonete

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class AdicionarSetorActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private var setorId: String? = null // Guarda o ID no modo "Editar"
    private lateinit var tvTitulo: TextView
    private lateinit var etNomeSetor: TextInputEditText
    private lateinit var btnSalvar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adicionar_setor)
        db = FirebaseFirestore.getInstance()
        tvTitulo = findViewById(R.id.tvTituloFormularioSetor)
        etNomeSetor = findViewById(R.id.etNomeSetor)
        btnSalvar = findViewById(R.id.btnSalvarSetor)
        setorId = intent.getStringExtra("SETOR_ID")

        if (setorId == null) {
            tvTitulo.text = "Novo Setor"
            btnSalvar.text = "Salvar Setor"
        } else {
            tvTitulo.text = "Editar Setor"
            btnSalvar.text = "Atualizar Setor"
            carregarDadosDoSetor(setorId!!)
        }
        btnSalvar.setOnClickListener {
            validarESalvarSetor()
        }
    }
    private fun carregarDadosDoSetor(id: String) {
        db.collection("setores").document(id)
            .get()
            .addOnSuccessListener { document ->
                val setor = document.toObject(Setor::class.java)
                if (setor != null) {
                    etNomeSetor.setText(setor.nome)
                } else {
                    Toast.makeText(this, "Setor não encontrado.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar dados.", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
    private fun validarESalvarSetor() {
        val nome = etNomeSetor.text.toString().trim()
        if (nome.isEmpty()) {
            etNomeSetor.error = "O nome do setor é obrigatório"
            return
        }
        val setor = Setor(
            id = setorId ?: "", // Usa o ID existente ou "" se for novo
            nome = nome
        )
        if (setorId == null) {
            salvarNovoSetor(setor)
        } else {
            atualizarSetor(setor)
        }
    }
    private fun salvarNovoSetor(setor: Setor) {
        db.collection("setores")
            .add(setor)
            .addOnSuccessListener {
                Toast.makeText(this, "Setor salvo com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun atualizarSetor(setor: Setor) {
        db.collection("setores").document(setor.id)
            .set(setor)
            .addOnSuccessListener {
                Toast.makeText(this, "Setor atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao atualizar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}