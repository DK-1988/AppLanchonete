package com.example.applanchonete

data class ModificadorGrupo(
    var id: String = "",
    val nome: String = "",
    val tipoSelecao: String = "MULTIPLA",
    val obrigatorio: Boolean = false,
    val opcoes: List<ModificadorOpcao> = emptyList()
)