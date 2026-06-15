package com.example.vetapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class VacunaAdapter(
    private var lista: MutableList<Vacuna>
) : RecyclerView.Adapter<VacunaAdapter.VacunaViewHolder>() {

    inner class VacunaViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val cardVacuna: MaterialCardView = view.findViewById(R.id.cardVacuna)

        val tvNombreVacuna: TextView =
            view.findViewById(R.id.tvNombreVacuna)

        val tvFechaAplicacion: TextView =
            view.findViewById(R.id.tvFechaAplicacion)

        val tvFechaProxima: TextView =
            view.findViewById(R.id.tvFechaProxima)

        val tvLote: TextView =
            view.findViewById(R.id.tvLote)

        val tvNotas: TextView =
            view.findViewById(R.id.tvNotas)

        val tvEstado: TextView =
            view.findViewById(R.id.tvEstado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VacunaViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vacuna, parent, false)

        return VacunaViewHolder(view)
    }

    override fun onBindViewHolder(holder: VacunaViewHolder, position: Int) {

        val vacuna = lista[position]

        holder.tvNombreVacuna.text = "💉 ${vacuna.nombre}"
        holder.tvFechaAplicacion.text =
            "Aplicada: ${formatearFecha(vacuna.fechaAplicacion)}"

        holder.tvFechaProxima.text =
            "Próxima: ${formatearFecha(vacuna.fechaProxima)}"

        holder.tvLote.text =
            "Lote: ${vacuna.lote ?: "No registrado"}"

        holder.tvNotas.text =
            "Notas: ${vacuna.notas ?: "Sin notas"}"

        // -------- Estado --------

        when (obtenerEstado(vacuna.fechaProxima)) {

            EstadoVacuna.VIGENTE -> {

                holder.tvEstado.text = "🟢 Vigente"
                holder.tvEstado.setTextColor(Color.parseColor("#2E7D32"))

                holder.cardVacuna.strokeWidth = 4
                holder.cardVacuna.strokeColor =
                    Color.parseColor("#66BB6A")
            }

            EstadoVacuna.PROXIMA -> {

                holder.tvEstado.text = "🟡 Próxima a vencer"
                holder.tvEstado.setTextColor(Color.parseColor("#F9A825"))

                holder.cardVacuna.strokeWidth = 4
                holder.cardVacuna.strokeColor =
                    Color.parseColor("#FDD835")
            }

            EstadoVacuna.VENCIDA -> {

                holder.tvEstado.text = "🔴 Vencida"
                holder.tvEstado.setTextColor(Color.parseColor("#C62828"))

                holder.cardVacuna.strokeWidth = 4
                holder.cardVacuna.strokeColor =
                    Color.parseColor("#EF5350")
            }
        }
    }

    override fun getItemCount(): Int = lista.size

    fun actualizar(nuevaLista: List<Vacuna>) {

        lista = nuevaLista.toMutableList()

        notifyDataSetChanged()
    }

    // =====================================================
    // ESTADO DE LA VACUNA
    // =====================================================

    private enum class EstadoVacuna {
        VIGENTE,
        PROXIMA,
        VENCIDA
    }

    private fun obtenerEstado(fechaProxima: String): EstadoVacuna {

        return try {

            val formato = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )

            val fecha: Date = formato.parse(fechaProxima)!!

            val hoy = Calendar.getInstance()

            val limite = Calendar.getInstance()
            limite.add(Calendar.DAY_OF_YEAR, 30)

            when {

                fecha.before(hoy.time) ->
                    EstadoVacuna.VENCIDA

                fecha.before(limite.time) ->
                    EstadoVacuna.PROXIMA

                else ->
                    EstadoVacuna.VIGENTE
            }

        } catch (e: Exception) {

            EstadoVacuna.VIGENTE
        }
    }

    // =====================================================
    // FORMATEAR FECHA
    // =====================================================

    private fun formatearFecha(fecha: String): String {

        return try {

            val origen = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )

            val destino = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

            destino.format(origen.parse(fecha)!!)

        } catch (e: Exception) {

            fecha
        }
    }
}