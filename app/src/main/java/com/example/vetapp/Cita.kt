package com.example.vetapp

data class Cita(
    val id: Int,
    val mascota: String,
    val especie: String,
    val raza: String?,
    val edad: Int?,
    val motivo: String,
    val fecha: String,   // formato: YYYY-MM-DD
    val hora: String,     // formato: HH:mm:ss
    val pagada: Boolean = false,
    val costo: Double = 350.0
)
