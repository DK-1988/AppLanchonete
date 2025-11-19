package com.example.applanchonete

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class RegistrarUsuarioActivity : AppCompatActivity() {

    private val TAG = "RegistrarUsuario"

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etEmail: TextInputEditText
    private lateinit var etSenha: TextInputEditText
    private lateinit var etConfirmarSenha: TextInputEditText

    private lateinit var rbAdmin: RadioButton

    private lateinit var btnRegistrar: Button
    private lateinit var btnIrParaLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar_usuario)

        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()

        etEmail = findViewById(R.id.etEmailRegistro)
        etSenha = findViewById(R.id.etSenhaRegistro)
        etConfirmarSenha = findViewById(R.id.etConfirmarSenhaRegistro)

        rbAdmin = findViewById(R.id.rbAdmin)

        btnRegistrar = findViewById(R.id.btnRegistrar)
        btnIrParaLogin = findViewById(R.id.btnIrParaLogin)

        btnRegistrar.setOnClickListener {
            registrarNovoUsuario()
        }

        btnIrParaLogin.setOnClickListener {
            finish()
        }
    }

    private fun registrarNovoUsuario() {
        val email = etEmail.text.toString().trim()
        val senha = etSenha.text.toString().trim()
        val confirmarSenha = etConfirmarSenha.text.toString().trim()
        if (email.isEmpty()) { etEmail.error = "E-mail é obrigatório"; etEmail.requestFocus(); return }
        if (senha.isEmpty()) { etSenha.error = "Senha é obrigatória"; etSenha.requestFocus(); return }
        if (senha.length < 6) { etSenha.error = "Senha deve ter no mínimo 6 caracteres"; etSenha.requestFocus(); return }
        if (confirmarSenha.isEmpty()) { etConfirmarSenha.error = "Confirmação é obrigatória"; etConfirmarSenha.requestFocus(); return }
        if (senha != confirmarSenha) { etConfirmarSenha.error = "As senhas não são iguais"; etConfirmarSenha.requestFocus(); return }

        val tipoUsuarioEscolhido = if (rbAdmin.isChecked) "Admin" else "Caixa"

        auth.createUserWithEmailAndPassword(email, senha)
            .addOnSuccessListener { authResult ->
                Toast.makeText(this, "Usuário criado com sucesso!", Toast.LENGTH_SHORT).show()

                val uid = authResult.user?.uid
                if (uid != null) {
                    val novoUsuario = Usuario(
                        uid = uid,
                        email = email,
                        tipoUsuario = tipoUsuarioEscolhido
                    )

                    db.collection("usuarios").document(uid)
                        .set(novoUsuario)
                        .addOnSuccessListener {
                            Log.d(TAG, "Usuário salvo como: $tipoUsuarioEscolhido")
                            Toast.makeText(this, "Registrado como $tipoUsuarioEscolhido", Toast.LENGTH_LONG).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Erro ao salvar dados", e)
                            Toast.makeText(this, "Erro ao salvar perfil: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                var msgErro = "Erro ao registrar: ${e.message}"
                if (e.message?.contains("email address is already in use") == true) {
                    msgErro = "Este e-mail já está em uso."
                }
                Toast.makeText(this, msgErro, Toast.LENGTH_LONG).show()
            }
    }
}