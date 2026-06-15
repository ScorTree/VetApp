package com.example.vetapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vetapp.R

class HistorialGeneralAdapter(
    private val lista: MutableList<HistorialGeneral>
) : RecyclerView.Adapter<HistorialGeneralAdapter.HistorialViewHolder>() {

    // ---------- ViewHolder ----------
    inner class HistorialViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvDiagnostico: TextView = view.findViewById(R.id.tvDiagnostico)
        val tvNotas: TextView = view.findViewById(R.id.tvNotas)
        val tvDatosExtra: TextView = view.findViewById(R.id.tvDatosExtra)
    }

    // ---------- Crear View ----------
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorialViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historial_general, parent, false)
        return HistorialViewHolder(view)
    }

    // ---------- Bind ----------
    override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {
        val item = lista[position]
        val context = holder.itemView.context

        holder.tvFecha.text = item.fecha

        holder.tvDiagnostico.text =
            item.diagnostico ?: context.getString(R.string.sin_diagnostico)

        holder.tvNotas.text =
            item.notas ?: context.getString(R.string.sin_notas)

        val pesoTxt = item.peso?.let {
            context.getString(R.string.peso_kg, it)
        } ?: context.getString(R.string.peso_no_disponible)

        val tempTxt = item.temperatura?.let {
            context.getString(R.string.temp_c, it)
        } ?: context.getString(R.string.temp_no_disponible)

        holder.tvDatosExtra.text = "$pesoTxt • $tempTxt"
    }

    // ---------- Count ----------
    override fun getItemCount(): Int = lista.size

    // ---------- Actualizar lista ----------
    fun setData(nuevaLista: List<HistorialGeneral>) {
        lista.clear()
        lista.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}
