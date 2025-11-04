package com.example.applanchonete

data class Produto(
    var id: String = "",
    val nome: String = "",
    val setor: String = "",
    val precoCusto: Double = 0.0,
    val margemLucro: Double = 0.0,
    val precoVenda: Double = 0.0,
    var quantidadeEstoque: Int = 0,
    val modificadorGrupoIds: List<String> = emptyList()
)