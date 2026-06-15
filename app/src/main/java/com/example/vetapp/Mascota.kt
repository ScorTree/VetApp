package com.example.vetapp

data class Mascota(
    val pacId: Int,
    val nombre: String,
    val especie: String,
    val raza: String?,
    val edad: Int?
)
