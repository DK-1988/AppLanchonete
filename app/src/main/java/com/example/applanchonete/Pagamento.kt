package com.example.applanchonete

import java.io.Serializable

data class Pagamento(
    val forma: String = "",
    val valor: Double = 0.0
) : Serializable