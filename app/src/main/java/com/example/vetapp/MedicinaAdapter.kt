package com.example.vetapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MedicinaAdapter(
    private var lista: MutableList<Medicina>
) : RecyclerView.Adapter<MedicinaAdapter.MedicinaViewHolder>() {

    inner class MedicinaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreMed)
        val tvDosis: TextView = view.findViewById(R.id.tvDosis)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvCosto: TextView = view.findViewById(R.id.tvCosto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicinaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicina, parent, false)
        return MedicinaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicinaViewHolder, position: Int) {
        val item = lista[position]
        holder.tvNombre.text = item.nombre
        holder.tvDosis.text = "Dosis: ${item.dosis}"
        holder.tvFecha.text = "Fecha: ${item.fecha}"
        holder.tvCosto.text = "Costo: $${item.costo}"
    }

    override fun getItemCount(): Int = lista.size

    fun actualizar(nuevaLista: List<Medicina>) {
        lista.clear()
        lista.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}
