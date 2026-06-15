package com.example.vetapp

data class PacienteVet(
    val pacId     : Int,
    val nombre    : String,
    val especie   : String,
    val raza      : String,
    val edad      : Int?,
    val propNombre: String,
    val propTel   : String
)