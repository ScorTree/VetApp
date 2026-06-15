package com.example.vetapp

data class HistorialGeneral(
    val histId      : Int,
    val fecha       : String,
    val peso        : Double?,
    val temp        : Double? = null,
    val temperatura : Double? = null,
    val diagnostico : String?,
    val notas       : String?
)