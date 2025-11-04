package com.example.applanchonete

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log // NOVO: Para Logs
import android.widget.Button
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser // NOVO
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore // NOVO
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etEmail: TextInputEditText
    private lateinit var etSenha: TextInputEditText
    private lateinit var btnEntrar: Button
    private lateinit var btnIrParaRegistro: Button

    override fun onStart() {
        super.onStart()
        val usuarioAtual = auth.currentUser
        if (usuarioAtual != null) {
            Toast.makeText(this, "Bem-vindo de volta!", Toast.LENGTH_SHORT).show()
            buscarTipoUsuarioEContinuar(usuarioAtual)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth
        db = FirebaseFirestore.getInstance() // NOVO

        etEmail = findViewById(R.id.etEmailLogin)
        etSenha = findViewById(R.id.etSenhaLogin)
        btnEntrar = findViewById(R.id.btnEntrar)
        btnIrParaRegistro = findViewById(R.id.btnIrParaRegistro)
        btnEntrar.setOnClickListener {
            fazerLoginUsuario()
        }

        btnIrParaRegistro.setOnClickListener {
            val intent = Intent(this, RegistrarUsuarioActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fazerLoginUsuario() {
        val email = etEmail.text.toString().trim()
        val senha = etSenha.text.toString().trim()

        if (email.isEmpty()) { etEmail.error = "E-mail é obrigatório"; etEmail.requestFocus(); return }
        if (senha.isEmpty()) { etSenha.error = "Senha é obrigatória"; etSenha.requestFocus(); return }

        auth.signInWithEmailAndPassword(email, senha)
            .addOnSuccessListener { authResult ->
                val usuario = authResult.user
                if (usuario != null) {
                    buscarTipoUsuarioEContinuar(usuario)
                } else {
                    Toast.makeText(this, "Erro ao obter dados do usuário.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                var msgErro = "Erro ao fazer login."
                if (e.message?.contains("INVALID_LOGIN_CREDENTIALS", true) == true) {
                    msgErro = "E-mail ou senha incorretos."
                }
                Log.w(TAG, "Falha no Login: ${e.message}")
                Toast.makeText(this, msgErro, Toast.LENGTH_LONG).show()
            }
    }

    private fun buscarTipoUsuarioEContinuar(usuario: FirebaseUser) {
        val uid = usuario.uid
        val email = usuario.email

        db.collection("usuarios").document(uid)
            .get()
            .addOnSuccessListener { document ->
                var tipoUsuario = "Caixa"

                if (document.exists()) {
                    val usuarioApp = document.toObject(Usuario::class.java)
                    tipoUsuario = usuarioApp?.tipoUsuario ?: "Caixa" // Pega o tipo ou "Caixa" se nulo
                    Log.d(TAG, "Usuário encontrado no Firestore. Tipo: $tipoUsuario")
                } else {
                    Log.w(TAG, "Usuário logado no Auth ($email) mas sem documento no Firestore. Criando documento padrão.")
                    val usuarioFantasma = Usuario(uid, email ?: "", "Caixa")
                    db.collection("usuarios").document(uid).set(usuarioFantasma)
                }

                Toast.makeText(this, "Login bem-sucedido!", Toast.LENGTH_SHORT).show()
                irParaMainActivity(tipoUsuario)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao buscar dados do usuário no Firestore: ${e.message}", e)
                Toast.makeText(this, "Aviso: Não foi possível verificar o nível de acesso. Logando como Caixa.", Toast.LENGTH_LONG).show()
                irParaMainActivity("Caixa")
            }
    }
    private fun irParaMainActivity(tipoUsuario: String) {
        val intent = Intent(this, MainActivity::class.java)

        intent.putExtra("TIPO_USUARIO", tipoUsuario)

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}