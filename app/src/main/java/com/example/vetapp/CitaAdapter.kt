package com.example.vetapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CitaAdapter(
    private val lista: List<Cita>,
    private val onLongClick: (Cita) -> Unit
) : RecyclerView.Adapter<CitaAdapter.CitaViewHolder>() {

    inner class CitaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardCita: MaterialCardView = view.findViewById(R.id.cardCita)
        val tvMascota: TextView = view.findViewById(R.id.tvMascota)
        val tvInfoMascota: TextView = view.findViewById(R.id.tvInfoMascota)
        val tvMotivo: TextView = view.findViewById(R.id.tvMotivo)
        val tvFechaHora: TextView = view.findViewById(R.id.tvFechaHora)
        val tvCosto: TextView = view.findViewById(R.id.tvCosto)
        val tvEstadoPago: TextView = view.findViewById(R.id.tvEstadoPago)
        val btnPagar: MaterialButton = view.findViewById(R.id.btnPagar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CitaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cita, parent, false)
        return CitaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CitaViewHolder, position: Int) {
        val cita = lista[position]
        val esPasada = citaEsPasada(cita)

        holder.tvMascota.text = cita.mascota

        holder.tvInfoMascota.text =
            "${cita.especie} • ${cita.raza ?: "Sin raza"} (${cita.edad ?: "?"} años)"

        holder.tvMotivo.text = "Motivo: ${cita.motivo}"

        holder.tvFechaHora.text =
            "Fecha: ${cita.fecha} a las ${cita.hora}"

        holder.tvCosto.text =
            "Costo: $${String.format(Locale.getDefault(), "%.2f", cita.costo)}"

        if (cita.pagada) {
            holder.tvEstadoPago.text = "🟢 Pagada"
            holder.tvEstadoPago.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
            holder.btnPagar.visibility = View.GONE
        } else {
            holder.tvEstadoPago.text = "🟠 Pendiente"
            holder.tvEstadoPago.setTextColor(android.graphics.Color.parseColor("#F57C00"))
            holder.btnPagar.visibility = View.VISIBLE
        }

        holder.btnPagar.setOnClickListener {
            val context = holder.itemView.context

            val intent = Intent(context, PagoActivity::class.java).apply {
                putExtra("citaId", cita.id)
                putExtra("mascota", cita.mascota)
                putExtra("motivo", cita.motivo)
                putExtra("fecha", cita.fecha)
                putExtra("hora", cita.hora)
                putExtra("costo", cita.costo)
            }

            context.startActivity(intent)
        }

        if (esPasada) {
            holder.cardCita.alpha = 0.45f
            holder.cardCita.isClickable = false
            holder.cardCita.isLongClickable = false
        } else {
            holder.cardCita.alpha = 1f
            holder.cardCita.setOnLongClickListener {
                onLongClick(cita)
                true
            }
        }
    }

    override fun getItemCount(): Int = lista.size

    private fun citaEsPasada(cita: Cita): Boolean {
        return try {
            val formato = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            )

            val fechaCita: Date =
                formato.parse("${cita.fecha} ${cita.hora}") ?: return false

            fechaCita.before(Date())

        } catch (e: Exception) {
            false
        }
    }
}