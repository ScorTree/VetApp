package com.example.vetapp

data class MedicamentoInventario(
    val medId    : Int,
    val nombre   : String,
    val lote     : String,
    val caducidad: String,
    val precio   : Double,
    val stock    : Int
)