package com.example.vetapp

data class CitaVet(
    val citaId    : Int,
    val fecha     : String,
    val hora      : String,
    val motivo    : String,
    val estado    : String,
    val pacId     : Int,
    val pacNombre : String,
    val pacEspecie: String,
    val pacRaza   : String,
    val pacEdad   : Int?    = null,
    val propNombre: String  = ""
)