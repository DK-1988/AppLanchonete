package com.example.applanchonete

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class SessaoCaixa(
    var id: String = "",
    val usuarioEmail: String = "",

    @ServerTimestamp
    val dataAbertura: Timestamp? = null,

    var dataFechamento: Timestamp? = null,

    val valorInicial: Double = 0.0,
    var valorTotalVendas: Double = 0.0,
    var valorFinalCalculado: Double = 0.0,
    var status: String = "Aberto"
)