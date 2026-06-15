package com.example.vetapp

data class Pago(
    val id: Int,
    val monto: Double,
    val fecha: String,
    val estado: String,
    val concepto: String,
    val citaId: Int?,
    val metodo: String?,
    val referencia: String?
)