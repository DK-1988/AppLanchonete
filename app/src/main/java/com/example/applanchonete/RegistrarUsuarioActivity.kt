package com.example.applanchonete

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log // NOVO: Para Logs
import android.widget.Button
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class RegistrarUsuarioActivity : AppCompatActivity() {

    private val TAG = "RegistrarUsuario" // NOVO: Para Logs

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Componentes de UI
    private lateinit var etEmail: TextInputEditText
    private lateinit var etSenha: TextInputEditText
    private lateinit var etConfirmarSenha: TextInputEditText
    private lateinit var btnRegistrar: Button
    private lateinit var btnIrParaLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar_usuario)

        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()

        // Ligar componentes
        etEmail = findViewById(R.id.etEmailRegistro)
        etSenha = findViewById(R.id.etSenhaRegistro)
        etConfirmarSenha = findViewById(R.id.etConfirmarSenhaRegistro)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        btnIrParaLogin = findViewById(R.id.btnIrParaLogin)

        btnRegistrar.setOnClickListener {
            registrarNovoUsuario()
        }

        btnIrParaLogin.setOnClickListener {
            finish()
        }
    }

    /**
     * --- registrarNovoUsuario (MODIFICADO) ---
     * Verifica se é o primeiro usuário para definir como Admin.
     */
    private fun registrarNovoUsuario() {
        // 1. Pega os textos
        val email = etEmail.text.toString().trim()
        val senha = etSenha.text.toString().trim()
        val confirmarSenha = etConfirmarSenha.text.toString().trim()

        // 2. Validação dos campos (sem mudança)
        if (email.isEmpty()) { etEmail.error = "E-mail é obrigatório"; etEmail.requestFocus(); return }
        if (senha.isEmpty()) { etSenha.error = "Senha é obrigatória"; etSenha.requestFocus(); return }
        if (senha.length < 6) { etSenha.error = "Senha deve ter no mínimo 6 caracteres"; etSenha.requestFocus(); return }
        if (confirmarSenha.isEmpty()) { etConfirmarSenha.error = "Confirmação é obrigatória"; etConfirmarSenha.requestFocus(); return }
        if (senha != confirmarSenha) { etConfirmarSenha.error = "As senhas não são iguais"; etConfirmarSenha.requestFocus(); return }

        // --- NOVO (Passo 136 - Correção): Verifica se já existem usuários ---
        // 3. Faz uma consulta ao Firestore ANTES de criar o usuário no Auth
        db.collection("usuarios")
            .limit(1) // Só precisamos saber se existe pelo menos 1
            .get()
            .addOnSuccessListener { snapshot ->

                // 4. Determina o tipo do usuário
                val tipoUsuarioPadrao = if (snapshot.isEmpty) {
                    // Coleção "usuarios" está vazia. Este é o primeiro usuário.
                    Log.i(TAG, "Nenhum usuário encontrado. Definindo novo usuário como Admin.")
                    "Admin"
                } else {
                    // Coleção "usuarios" já tem usuários.
                    Log.i(TAG, "Usuários existentes encontrados. Definindo novo usuário como Caixa.")
                    "Caixa"
                }

                // 5. Agora sim, tenta criar o usuário no Auth
                auth.createUserWithEmailAndPassword(email, senha)
                    .addOnSuccessListener { authResult ->
                        // Sucesso no Auth!
                        Toast.makeText(this, "Usuário registrado com sucesso!", Toast.LENGTH_SHORT).show()

                        val uid = authResult.user?.uid
                        if (uid != null) {
                            // 6. Cria o objeto Usuario com o tipo determinado
                            val novoUsuario = Usuario(
                                uid = uid,
                                email = email,
                                tipoUsuario = tipoUsuarioPadrao // Usa a variável
                            )

                            // 7. Salva na coleção "usuarios"
                            db.collection("usuarios").document(uid)
                                .set(novoUsuario)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Usuário salvo no Firestore com tipo: $tipoUsuarioPadrao")
                                    finish() // Sucesso total! Volta ao Login.
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Erro ao salvar usuário no Firestore", e)
                                    Toast.makeText(this, "Erro ao salvar dados do usuário: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        } else {
                            Toast.makeText(this, "Erro ao obter UID do usuário.", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        // Falha ao criar no Auth (ex: e-mail já existe)
                        var msgErro = "Erro ao registrar: ${e.message}"
                        if (e.message?.contains("email address is already in use") == true) {
                            msgErro = "Este e-mail já está em uso."
                            etEmail.error = msgErro
                            etEmail.requestFocus()
                        }
                        Toast.makeText(this, msgErro, Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                // Falha grave: Não conseguiu nem checar o Firestore (ex: sem internet)
                Log.e(TAG, "Erro ao verificar coleção 'usuarios'", e)
                Toast.makeText(this, "Erro ao verificar usuários. Tente novamente. ${e.message}", Toast.LENGTH_LONG).show()
            }
        // --- FIM DA MODIFICAÇÃO ---
    }
}