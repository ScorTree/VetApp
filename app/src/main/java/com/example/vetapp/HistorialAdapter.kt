package com.example.vetapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistorialAdapter(
    private val lista: MutableList<HistorialGeneral>
) : RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder>() {

    inner class HistorialViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvDiagnostico: TextView = view.findViewById(R.id.tvDiagnostico)
        val tvNotas: TextView = view.findViewById(R.id.tvNotas)
        val tvDatosExtra: TextView = view.findViewById(R.id.tvDatosExtra)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorialViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historial_general, parent, false)
        return HistorialViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {
        val item = lista[position]

        holder.tvFecha.text = item.fecha

        holder.tvDiagnostico.text =
            item.diagnostico ?: holder.itemView.context.getString(R.string.sin_diagnostico)

        holder.tvNotas.text =
            item.notas ?: holder.itemView.context.getString(R.string.sin_notas)

        val pesoTxt =
            item.peso?.let { "Peso: ${it} kg" } ?: "Peso: -"

        val tempTxt =
            item.temperatura?.let { "Temp: ${it} °C" } ?: "Temp: -"

        holder.tvDatosExtra.text = "$pesoTxt • $tempTxt"
    }

    override fun getItemCount(): Int = lista.size

    fun actualizar(nuevaLista: List<HistorialGeneral>) {
        lista.clear()
        lista.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}
