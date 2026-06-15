package com.example.vetapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Locale

class PagoAdapter(
    private val lista: MutableList<Pago>
) : RecyclerView.Adapter<PagoAdapter.PagoViewHolder>() {

    inner class PagoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardPago: MaterialCardView = view.findViewById(R.id.cardPago)
        val tvConcepto: TextView = view.findViewById(R.id.tvConcepto)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvMonto: TextView = view.findViewById(R.id.tvMonto)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
        val tvMetodo: TextView = view.findViewById(R.id.tvMetodo)
        val tvReferencia: TextView = view.findViewById(R.id.tvReferencia)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pago, parent, false)

        return PagoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PagoViewHolder, position: Int) {
        val pago = lista[position]

        holder.tvConcepto.text = pago.concepto
        holder.tvFecha.text = "Fecha: ${formatearFecha(pago.fecha)}"
        holder.tvMonto.text = "Monto: $${String.format(Locale.getDefault(), "%.2f", pago.monto)}"
        holder.tvMetodo.text = "Método: ${pago.metodo ?: "No especificado"}"
        holder.tvReferencia.text = "Referencia: ${pago.referencia ?: "Sin referencia"}"

        if (pago.estado.equals("Pagado", ignoreCase = true)) {
            holder.tvEstado.text = "🟢 Pagado"
            holder.tvEstado.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            holder.tvEstado.text = "🟠 ${pago.estado}"
            holder.tvEstado.setTextColor(Color.parseColor("#F57C00"))
        }
    }

    override fun getItemCount(): Int = lista.size

    fun actualizar(nuevaLista: List<Pago>) {
        lista.clear()
        lista.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    private fun formatearFecha(fecha: String): String {
        return try {
            val origen = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val destino = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            destino.format(origen.parse(fecha)!!)
        } catch (e: Exception) {
            fecha
        }
    }
}