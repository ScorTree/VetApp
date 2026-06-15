package com.example.vetapp

data class MedicamentoSeleccionado(
    val medId    : Int,
    val nombre   : String,
    var cantidad : Int,
    var dosis    : String
)