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

class DiagnosticoActivity : AppCompatActivity() {

    private var citaId  = -1
    private var pacId   = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostico)

        citaId = intent.getIntExtra("citaId", -1)
        pacId  = intent.getIntExtra("pacId",  -1)
        val pacNombre = intent.getStringExtra("pacNombre") ?: ""
        val motivo    = intent.getStringExtra("motivo")    ?: ""

        findViewById<TextView>(R.id.tvDiagPaciente).text = pacNombre
        findViewById<TextView>(R.id.tvDiagMotivo).text   = "Motivo: $motivo"

        val etPeso  = findViewById<TextInputEditText>(R.id.etDiagPeso)
        val etTemp  = findViewById<TextInputEditText>(R.id.etDiagTemp)
        val etDiag  = findViewById<TextInputEditText>(R.id.etDiagDiagnostico)
        val etNotas = findViewById<TextInputEditText>(R.id.etDiagNotas)

        findViewById<MaterialButton>(R.id.btnGuardarDiag).setOnClickListener {
            val peso  = etPeso.text.toString().trim()
            val temp  = etTemp.text.toString().trim()
            val diag  = etDiag.text.toString().trim()
            val notas = etNotas.text.toString().trim()

            if (diag.isEmpty()) {
                Toast.makeText(this, "El diagnóstico es obligatorio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            guardarDiagnostico(peso, temp, diag, notas)
        }

        findViewById<MaterialButton>(R.id.btnIrPrescripcion).setOnClickListener {
            val intent = Intent(this, PrescripcionActivity::class.java)
            intent.putExtra("citaId",    citaId)
            intent.putExtra("pacId",     pacId)
            intent.putExtra("pacNombre", pacNombre)
            startActivity(intent)
        }

        findViewById<android.widget.ImageButton>(R.id.btnBackDiag).setOnClickListener {
            finish()
        }
    }

    private fun guardarDiagnostico(
        peso: String, temp: String, diag: String, notas: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cal = java.util.Calendar.getInstance()
                val hoy = String.format(
                    java.util.Locale.getDefault(),
                    "%04d-%02d-%02d",
                    cal.get(java.util.Calendar.YEAR),
                    cal.get(java.util.Calendar.MONTH) + 1,
                    cal.get(java.util.Calendar.DAY_OF_MONTH)
                )

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

                // Cambiar estado de la cita a confirmada
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
                    Toast.makeText(this@DiagnosticoActivity,
                        "Diagnóstico guardado ✓", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@DiagnosticoActivity,
                        "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}