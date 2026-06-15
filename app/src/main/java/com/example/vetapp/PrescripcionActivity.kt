package com.example.vetapp

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import com.google.android.material.button.MaterialButton
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class PrescripcionActivity : AppCompatActivity() {

    private var citaId = -1
    private var pacId  = -1
    private val medicamentosDisponibles = mutableListOf<Medicamento>()
    private val medicamentosSeleccionados = mutableListOf<MedicamentoSeleccionado>()
    private lateinit var rvMeds: RecyclerView
    private lateinit var rvSeleccionados: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prescripcion)

        citaId = intent.getIntExtra("citaId", -1)
        pacId  = intent.getIntExtra("pacId",  -1)
        val pacNombre = intent.getStringExtra("pacNombre") ?: ""

        findViewById<TextView>(R.id.tvPrescPaciente).text = pacNombre

        rvMeds         = findViewById(R.id.rvMedicamentos)
        rvSeleccionados = findViewById(R.id.rvSeleccionados)

        rvMeds.layoutManager          = LinearLayoutManager(this)
        rvSeleccionados.layoutManager = LinearLayoutManager(this)

        findViewById<android.widget.ImageButton>(R.id.btnBackPresc).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnGuardarPresc).setOnClickListener {
            guardarPrescripcion()
        }

        cargarMedicamentos()
    }

    private fun cargarMedicamentos() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/medicamentos"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("med_activo", "eq.true")
                    parameter("select", "med_id,med_nombre,med_stock,med_precio")
                    parameter("order", "med_nombre.asc")
                }

                val arr = JSONArray(res.bodyAsText())
                medicamentosDisponibles.clear()

                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    medicamentosDisponibles.add(
                        Medicamento(
                            medId     = o.getInt("med_id"),
                            nombre    = o.getString("med_nombre"),
                            stock     = o.getInt("med_stock"),
                            precio    = o.optDouble("med_precio", 0.0)
                        )
                    )
                }

                runOnUiThread { mostrarMedicamentos() }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@PrescripcionActivity,
                        "Error al cargar medicamentos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarMedicamentos() {
        rvMeds.adapter = MedicamentoAdapter(medicamentosDisponibles) { med ->
            val yaEsta = medicamentosSeleccionados.any { it.medId == med.medId }
            if (yaEsta) {
                Toast.makeText(this, "${med.nombre} ya está en la receta",
                    Toast.LENGTH_SHORT).show()
                return@MedicamentoAdapter
            }
            medicamentosSeleccionados.add(
                MedicamentoSeleccionado(med.medId, med.nombre, 1, "1 vez al día por 7 días")
            )
            actualizarSeleccionados()
        }
    }

    private fun actualizarSeleccionados() {
        rvSeleccionados.adapter =
            MedicamentoSeleccionadoAdapter(medicamentosSeleccionados) { pos ->
                medicamentosSeleccionados.removeAt(pos)
                actualizarSeleccionados()
            }
        findViewById<TextView>(R.id.tvContadorPresc).text =
            "${medicamentosSeleccionados.size} medicamento(s) en receta"
    }

    private fun guardarPrescripcion() {
        if (medicamentosSeleccionados.isEmpty()) {
            Toast.makeText(this, "Agrega al menos un medicamento", Toast.LENGTH_SHORT).show()
            return
        }
        if (citaId == -1) {
            Toast.makeText(this, "Error: cita no identificada", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                var exito = true
                for (med in medicamentosSeleccionados) {
                    val body = """
                    {
                      "cita_id":$citaId,
                      "med_id":${med.medId},
                      "presc_dosis":"${med.dosis}",
                      "presc_cantidad":${med.cantidad},
                      "presc_indicaciones":"${med.dosis}"
                    }
                    """.trimIndent()

                    val res = HttpClientProvider.client.post(
                        "${SupabaseConfig.URL}/rest/v1/prescripciones"
                    ) {
                        headers {
                            append("apikey", SupabaseConfig.API_KEY)
                            append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                            append(HttpHeaders.ContentType, "application/json")
                            append("Prefer", "return=minimal")
                        }
                        setBody(body)
                    }
                    if (res.status.value !in 200..299) exito = false
                }

                runOnUiThread {
                    if (exito) {
                        Toast.makeText(this@PrescripcionActivity,
                            "Prescripción guardada ✓ Stock actualizado", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@PrescripcionActivity,
                            "Algunos medicamentos no se guardaron", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@PrescripcionActivity,
                        "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}