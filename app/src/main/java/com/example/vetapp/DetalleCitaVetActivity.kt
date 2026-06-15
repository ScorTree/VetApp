package com.example.vetapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class DetalleCitaVetActivity : AppCompatActivity() {

    private var citaId   = -1
    private var pacId    = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_cita_vet)

        citaId = intent.getIntExtra("citaId", -1)
        pacId  = intent.getIntExtra("pacId",  -1)
        val pacNombre = intent.getStringExtra("pacNombre") ?: ""
        val motivo    = intent.getStringExtra("motivo")    ?: ""

        val tvNombre  = findViewById<TextView>(R.id.tvDetCitaNombre)
        val tvEstado  = findViewById<TextView>(R.id.tvDetCitaEstado)
        val etPeso    = findViewById<TextInputEditText>(R.id.etDetPeso)
        val etTemp    = findViewById<TextInputEditText>(R.id.etDetTemp)
        val etDiag    = findViewById<TextInputEditText>(R.id.etDetDiagnostico)

        tvNombre.text = pacNombre

        // Cargar datos actuales del paciente
        cargarDatosPaciente(etPeso)

        findViewById<android.widget.ImageButton>(R.id.btnBackDetCita)
            .setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnGuardarDetCita).setOnClickListener {
            val peso = etPeso.text.toString().trim()
            val temp = etTemp.text.toString().trim()
            val diag = etDiag.text.toString().trim()

            if (diag.isEmpty()) {
                Toast.makeText(this, "El diagnóstico es obligatorio",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            guardarDiagnostico(peso, temp, diag)
        }

        findViewById<MaterialButton>(R.id.btnIrPrescripcionDet).setOnClickListener {
            val intent = Intent(this, PrescripcionActivity::class.java)
            intent.putExtra("citaId",    citaId)
            intent.putExtra("pacId",     pacId)
            intent.putExtra("pacNombre", pacNombre)
            startActivity(intent)
        }
    }

    private fun cargarDatosPaciente(etPeso: TextInputEditText) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/pacientes"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("pac_id", "eq.$pacId")
                    parameter("select", "pac_nombre,pac_especie,pac_edad,pac_raza")
                }
                val arr = JSONArray(res.bodyAsText())
                if (arr.length() > 0) {
                    val o = arr.getJSONObject(0)
                    val nombre  = o.getString("pac_nombre")
                    val especie = o.getString("pac_especie")
                    val edad    = o.optInt("pac_edad", 0)
                    val raza    = o.optString("pac_raza", "")

                    runOnUiThread {
                        findViewById<TextView>(R.id.tvDetCitaEspecie).text = especie
                        findViewById<TextView>(R.id.tvDetCitaEdad).text    = "$edad años"
                        findViewById<TextView>(R.id.tvDetCitaRaza).text    = raza
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun guardarDiagnostico(peso: String, temp: String, diag: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val hoy = java.time.LocalDate.now().toString()
                val body = buildString {
                    append("{")
                    append("\"hist_fecha\":\"$hoy\",")
                    append("\"hist_diag\":\"$diag\",")
                    append("\"pac_id\":$pacId")
                    if (peso.isNotEmpty()) append(",\"hist_peso\":$peso")
                    if (temp.isNotEmpty()) append(",\"hist_temp\":$temp")
                    append("}")
                }

                HttpClientProvider.client.post(
                    "${SupabaseConfig.URL}/rest/v1/historial_general"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                        append(HttpHeaders.ContentType, "application/json")
                        append("Prefer", "return=minimal")
                    }
                    setBody(body)
                }

                // Actualizar peso en paciente si se capturó
                if (peso.isNotEmpty()) {
                    HttpClientProvider.client.patch(
                        "${SupabaseConfig.URL}/rest/v1/pacientes"
                    ) {
                        headers {
                            append("apikey", SupabaseConfig.API_KEY)
                            append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                            append(HttpHeaders.ContentType, "application/json")
                            append("Prefer", "return=minimal")
                        }
                        parameter("pac_id", "eq.$pacId")
                        setBody("""{"pac_peso":$peso}""")
                    }
                }

                // Cambiar estado cita a confirmada
                HttpClientProvider.client.patch(
                    "${SupabaseConfig.URL}/rest/v1/citas"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                        append(HttpHeaders.ContentType, "application/json")
                        append("Prefer", "return=minimal")
                    }
                    parameter("cita_id", "eq.$citaId")
                    setBody("""{"cita_estado":"confirmada"}""")
                }

                runOnUiThread {
                    Toast.makeText(this@DetalleCitaVetActivity,
                        "Diagnóstico guardado ✓", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@DetalleCitaVetActivity,
                        "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}