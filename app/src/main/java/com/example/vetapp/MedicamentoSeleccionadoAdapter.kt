package com.example.vetapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class MedicamentoSeleccionadoAdapter(
    private val lista: List<MedicamentoSeleccionado>,
    private val onEliminar: (Int) -> Unit
) : RecyclerView.Adapter<MedicamentoSeleccionadoAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val card    : MaterialCardView = v.findViewById(R.id.cardMedSel)
        val tvNombre: TextView = v.findViewById(R.id.tvMedSelNombre)
        val tvDosis : TextView = v.findViewById(R.id.tvMedSelDosis)
        val btnQuitar: TextView = v.findViewById(R.id.btnQuitarMed)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicamento_seleccionado, parent, false))

    override fun getItemCount() = lista.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val m = lista[pos]
        h.tvNombre.text = m.nombre
        h.tvDosis.text  = m.dosis
        h.btnQuitar.setOnClickListener { onEliminar(pos) }
    }
}