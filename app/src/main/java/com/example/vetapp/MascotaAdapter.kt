package com.example.vetapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MascotaAdapter(
    private val mascotas: List<Mascota>,
    private val onClick: (Mascota) -> Unit
) : RecyclerView.Adapter<MascotaAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvInfo: TextView = itemView.findViewById(R.id.tvInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mascota, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mascota = mascotas[position]

        holder.tvNombre.text = mascota.nombre
        holder.tvInfo.text =
            "${mascota.especie} • ${mascota.raza ?: "Sin raza"} • ${mascota.edad ?: "?"} años"

        holder.itemView.setOnClickListener {
            onClick(mascota)
        }
    }

    override fun getItemCount(): Int = mascotas.size
}
