package com.example.vetapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class PacienteVetAdapter(
    private val lista: List<PacienteVet>,
    private val onClick: (PacienteVet) -> Unit
) : RecyclerView.Adapter<PacienteVetAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val card      : MaterialCardView = v.findViewById(R.id.cardPacienteVet)
        val tvNombre  : TextView = v.findViewById(R.id.tvPacVetNombre)
        val tvEspecie : TextView = v.findViewById(R.id.tvPacVetEspecie)
        val tvEdad    : TextView = v.findViewById(R.id.tvPacVetEdad)
        val tvPropNombre: TextView = v.findViewById(R.id.tvPacVetPropNombre)
        val tvPropTel : TextView = v.findViewById(R.id.tvPacVetPropTel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_paciente_vet, parent, false))

    override fun getItemCount() = lista.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val p = lista[pos]
        h.tvNombre.text    = p.nombre
        h.tvEspecie.text   = "${p.especie} · ${p.raza}"
        h.tvEdad.text      = if (p.edad != null) "${p.edad} años" else "Edad desconocida"
        h.tvPropNombre.text = "👤 ${p.propNombre}"
        h.tvPropTel.text   = "📞 ${p.propTel}"
        h.card.setOnClickListener { onClick(p) }
    }
}