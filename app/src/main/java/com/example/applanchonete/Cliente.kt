package com.example.applanchonete

data class Cliente(
    var id: String = "",
    val tipoCliente: String = "PF", // "PF" ou "PJ"
    val nomeRazaoSocial: String = "",
    val nomeFantasia: String = "",
    val cpfCnpj: String = "",
    val telefone: String = "",
    val email: String = "",
    val endereco: String = ""
)