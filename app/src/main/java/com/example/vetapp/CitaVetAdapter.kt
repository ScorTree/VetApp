package com.example.vetapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class CitaVetAdapter(
    private val citas: List<CitaVet>,
    private val onClick: (CitaVet) -> Unit
) : RecyclerView.Adapter<CitaVetAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val card       : MaterialCardView = v.findViewById(R.id.cardCitaVet)
        val tvNombre   : TextView = v.findViewById(R.id.tvCitaVetNombre)
        val tvHora     : TextView = v.findViewById(R.id.tvCitaVetHora)
        val tvDueno    : TextView = v.findViewById(R.id.tvCitaVetDueno)
        val tvMotivo   : TextView = v.findViewById(R.id.tvCitaVetMotivo)
        val tvFechaHora: TextView = v.findViewById(R.id.tvCitaVetFechaHora)
        val tvEspecie  : TextView = v.findViewById(R.id.tvCitaVetEspecie)
        val tvEstado   : TextView = v.findViewById(R.id.tvCitaVetEstado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cita_vet, parent, false))

    override fun getItemCount() = citas.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val c = citas[pos]
        h.tvNombre.text    = c.pacNombre
        h.tvHora.text      = c.hora
        h.tvDueno.text     = "Dueño: ${c.propNombre}"
        h.tvMotivo.text    = "Motivo: ${c.motivo}"
        h.tvFechaHora.text = "Fecha: ${formatearFecha(c.fecha)}"
        val edadStr = if (c.pacEdad != null) " - ${c.pacEdad} años" else ""
        val razaStr = if (c.pacRaza.isNotEmpty()) " - ${c.pacRaza}" else ""
        h.tvEspecie.text   = "${c.pacEspecie}$razaStr$edadStr"

        h.tvEstado.text = c.estado.replaceFirstChar { it.uppercase() }
        h.tvEstado.setBackgroundResource(
            when (c.estado) {
                "confirmada"  -> R.drawable.chip_confirmada
                "pagada"      -> R.drawable.chip_pagada
                "completada"  -> R.drawable.chip_pagada
                "cancelada"   -> R.drawable.chip_cancelada
                else          -> R.drawable.chip_pendiente
            }
        )
        h.card.setOnClickListener { onClick(c) }
    }

    private fun formatearFecha(fecha: String): String {
        return try {
            val parts = fecha.split("-")
            val meses = listOf("","enero","febrero","marzo","abril","mayo","junio",
                "julio","agosto","septiembre","octubre","noviembre","diciembre")
            "${parts[2]} ${meses[parts[1].toInt()]}"
        } catch (e: Exception) { fecha }
    }
}