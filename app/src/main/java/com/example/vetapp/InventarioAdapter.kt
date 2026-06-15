package com.example.vetapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class InventarioAdapter(
    private val lista: List<MedicamentoInventario>,
    private val onEditar  : (MedicamentoInventario) -> Unit,
    private val onEliminar: (MedicamentoInventario) -> Unit
) : RecyclerView.Adapter<InventarioAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val card      : MaterialCardView = v.findViewById(R.id.cardInventario)
        val tvNombre  : TextView = v.findViewById(R.id.tvInvNombre)
        val tvLote    : TextView = v.findViewById(R.id.tvInvLote)
        val tvCaducidad: TextView = v.findViewById(R.id.tvInvCaducidad)
        val tvPrecio  : TextView = v.findViewById(R.id.tvInvPrecio)
        val tvStock   : TextView = v.findViewById(R.id.tvInvStock)
        val btnEditar : TextView = v.findViewById(R.id.btnInvEditar)
        val btnEliminar: TextView = v.findViewById(R.id.btnInvEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inventario, parent, false))

    override fun getItemCount() = lista.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val m = lista[pos]
        h.tvNombre.text    = m.nombre
        h.tvLote.text      = "Lote: ${m.lote}"
        h.tvCaducidad.text = "Cad: ${m.caducidad}"
        h.tvPrecio.text    = "$${m.precio}"
        h.tvStock.text     = "Stock: ${m.stock}"

        // Color stock crítico
        if (m.stock < 5) {
            h.tvStock.setTextColor(Color.parseColor("#FF5252"))
            h.card.strokeColor = Color.parseColor("#FF5252")
            h.card.strokeWidth = 2
        } else {
            h.tvStock.setTextColor(Color.parseColor("#4CAF50"))
            h.card.strokeWidth = 0
        }

        h.btnEditar.setOnClickListener  { onEditar(m) }
        h.btnEliminar.setOnClickListener { onEliminar(m) }
    }
}