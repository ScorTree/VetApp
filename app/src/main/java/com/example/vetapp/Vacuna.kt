package com.example.vetapp

data class Vacuna(
    val id: Int,
    val nombre: String,
    val fechaAplicacion: String,
    val fechaProxima: String,
    val lote: String?,
    val notas: String?
)
