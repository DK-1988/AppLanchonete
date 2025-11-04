package com.example.applanchonete

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions // Import necessário para o .set(..., SetOptions.merge())

class EmpresaActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private val docRef by lazy { db.collection("configuracoes").document("empresa") }
    private lateinit var etNomeFantasia: TextInputEditText
    private lateinit var etRazaoSocial: TextInputEditText
    private lateinit var etCnpj: TextInputEditText
    private lateinit var etTelefone: TextInputEditText
    private lateinit var etEndereco: TextInputEditText
    private lateinit var btnSalvar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empresa)

        db = FirebaseFirestore.getInstance()

        etNomeFantasia = findViewById(R.id.etNomeFantasiaEmpresa)
        etRazaoSocial = findViewById(R.id.etRazaoSocialEmpresa)
        etCnpj = findViewById(R.id.etCnpjEmpresa)
        etTelefone = findViewById(R.id.etTelefoneEmpresa)
        etEndereco = findViewById(R.id.etEnderecoEmpresa)
        btnSalvar = findViewById(R.id.btnSalvarEmpresa)

        carregarDadosEmpresa()

        btnSalvar.setOnClickListener {
            validarESalvarDados()
        }
    }

    private fun carregarDadosEmpresa() {
        docRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Documento existe, preenche o formulário
                    val empresaInfo = documentSnapshot.toObject(EmpresaInfo::class.java)
                    if (empresaInfo != null) {
                        etNomeFantasia.setText(empresaInfo.nomeFantasia)
                        etRazaoSocial.setText(empresaInfo.razaoSocial)
                        etCnpj.setText(empresaInfo.cnpj)
                        etTelefone.setText(empresaInfo.telefone)
                        etEndereco.setText(empresaInfo.endereco)
                    }
                } else {
                    Log.d("EmpresaActivity", "Documento 'configuracoes/empresa' não encontrado.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("EmpresaActivity", "Erro ao carregar dados da empresa", e)
                Toast.makeText(this, "Erro ao carregar dados da empresa.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validarESalvarDados() {
        val nomeFantasia = etNomeFantasia.text.toString().trim()
        val razaoSocial = etRazaoSocial.text.toString().trim()
        val cnpj = etCnpj.text.toString().trim()
        val telefone = etTelefone.text.toString().trim()
        val endereco = etEndereco.text.toString().trim()

        if (nomeFantasia.isEmpty()) {
            etNomeFantasia.error = "Nome Fantasia é obrigatório"
            etNomeFantasia.requestFocus()
            return
        }
        if (cnpj.isEmpty()) {
            etCnpj.error = "CNPJ / CPF é obrigatório"
            etCnpj.requestFocus()
            return
        }
        if (telefone.isEmpty()) {
            etTelefone.error = "Telefone é obrigatório"
            etTelefone.requestFocus()
            return
        }
        if (endereco.isEmpty()) {
            etEndereco.error = "Endereço é obrigatório"
            etEndereco.requestFocus()
            return
        }

        val empresaInfo = EmpresaInfo(
            nomeFantasia = nomeFantasia,
            razaoSocial = razaoSocial,
            cnpj = cnpj,
            telefone = telefone,
            endereco = endereco
        )

        docRef.set(empresaInfo)
            .addOnSuccessListener {
                Toast.makeText(this, "Dados da empresa salvos com sucesso!", Toast.LENGTH_SHORT).show()
                finish() // Fecha a tela após salvar
            }
            .addOnFailureListener { e ->
                Log.e("EmpresaActivity", "Erro ao salvar dados da empresa", e)
                Toast.makeText(this, "Erro ao salvar dados: ${e.message}", Toast.LENGTH_LONG).show()
            }

    }
}