package com.example.vetapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class HistorialVetAdapter(
    private val lista: List<HistorialGeneral>
) : RecyclerView.Adapter<HistorialVetAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val card    : MaterialCardView = v.findViewById(R.id.cardHistVet)
        val tvFecha : TextView = v.findViewById(R.id.tvHistVetFecha)
        val tvDiag  : TextView = v.findViewById(R.id.tvHistVetDiag)
        val tvPeso  : TextView = v.findViewById(R.id.tvHistVetPeso)
        val tvTemp  : TextView = v.findViewById(R.id.tvHistVetTemp)
        val tvNotas : TextView = v.findViewById(R.id.tvHistVetNotas)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historial_vet, parent, false))

    override fun getItemCount() = lista.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = lista[pos]
        h.tvFecha.text = item.fecha
        h.tvDiag.text  = item.diagnostico

        h.tvPeso.text  = if (item.peso != null) "Peso: ${item.peso} kg" else ""
        h.tvTemp.text  = if (item.temp != null) "Temp: ${item.temp}°C"
        else if (item.temperatura != null) "Temp: ${item.temperatura}°C"
        else ""
        h.tvNotas.text = if (!item.notas.isNullOrEmpty()) "📝 ${item.notas}" else ""

        h.tvPeso.visibility  = if (item.peso != null) View.VISIBLE else View.GONE
        h.tvTemp.visibility  = if (item.temp != null || item.temperatura != null) View.VISIBLE else View.GONE
        h.tvNotas.visibility = if (!item.notas.isNullOrEmpty()) View.VISIBLE else View.GONE
    }
}