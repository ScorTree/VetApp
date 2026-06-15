package com.example.vetapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class MedicamentoAdapter(
    private val lista: List<Medicamento>,
    private val onClick: (Medicamento) -> Unit
) : RecyclerView.Adapter<MedicamentoAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val card    : MaterialCardView = v.findViewById(R.id.cardMed)
        val tvNombre: TextView = v.findViewById(R.id.tvMedNombre)
        val tvStock : TextView = v.findViewById(R.id.tvMedStock)
        val tvPrecio: TextView = v.findViewById(R.id.tvMedPrecio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicamento, parent, false))

    override fun getItemCount() = lista.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val m = lista[pos]
        h.tvNombre.text = m.nombre
        h.tvStock.text  = "Stock: ${m.stock}"
        h.tvPrecio.text = "$${m.precio}"

        val stockColor = if (m.stock < 5) "#FF5252" else "#4CAF50"
        h.tvStock.setTextColor(android.graphics.Color.parseColor(stockColor))

        h.card.setOnClickListener { onClick(m) }
    }
}