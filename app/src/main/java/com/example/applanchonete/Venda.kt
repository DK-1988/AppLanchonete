package com.example.applanchonete

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Venda(
    var id: String = "",
    @ServerTimestamp
    val dataHora: Timestamp? = null,
    val itens: List<ItemVenda> = emptyList(),
    val totalVenda: Double = 0.0,
    val pagamentos: List<Pagamento> = emptyList(),
    val sessaoCaixaId: String = "",
    val totalCusto: Double = 0.0,
    val lucroTotal: Double = 0.0,
    val clienteId: String = "",
    val clienteNome: String = "Cliente Padrão"
)