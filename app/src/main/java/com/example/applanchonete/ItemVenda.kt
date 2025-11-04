package com.example.applanchonete

data class ItemVenda(
    val produtoId: String = "",
    val nomeProduto: String = "",
    var quantidade: Int = 0,
    val precoUnitario: Double = 0.0,
    var totalItem: Double = 0.0,
    val precoCustoUnitario: Double = 0.0,
    val opcoesSelecionadas: List<ModificadorOpcao> = emptyList(),
    val precoAdicionais: Double = 0.0
)