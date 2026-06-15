package com.example.vetapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HorarioAdapter(
    private val lista: List<HorarioDisponible>,
    private val onClick: (HorarioDisponible) -> Unit
) : RecyclerView.Adapter<HorarioAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHora: TextView = view.findViewById(R.id.tvHora)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_horario, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val horario = lista[position]

        holder.tvHora.text = horario.hora

        if (horario.disponible) {
            holder.tvEstado.text = "Disponible"
            holder.tvEstado.setTextColor(Color.parseColor("#2E7D32"))
            holder.itemView.isEnabled = true
            holder.itemView.alpha = 1f

            holder.itemView.setOnClickListener {
                onClick(horario)
            }

        } else {
            holder.tvEstado.text = "Ocupado"
            holder.tvEstado.setTextColor(Color.parseColor("#C62828"))
            holder.itemView.isEnabled = false
            holder.itemView.alpha = 0.5f
        }
    }
}
