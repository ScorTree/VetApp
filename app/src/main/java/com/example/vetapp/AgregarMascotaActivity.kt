package com.example.vetapp

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import com.example.vetapp.util.SessionManager
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class AgregarMascotaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_mascota)

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etEspecie = findViewById<EditText>(R.id.etEspecie)
        val etRaza = findViewById<EditText>(R.id.etRaza)
        val etEdad = findViewById<EditText>(R.id.etEdad)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // REGRESAR A MAIN
        btnBack.setOnClickListener {
            finish()
        }

        btnGuardar.setOnClickListener {
            guardarMascota(
                etNombre.text.toString().trim(),
                etEspecie.text.toString().trim(),
                etRaza.text.toString().trim(),
                etEdad.text.toString().trim()
            )
        }
    }

    private fun guardarMascota(
        nombre: String,
        especie: String,
        raza: String,
        edadStr: String
    ) {

        if (nombre.isEmpty() || especie.isEmpty()) {
            Toast.makeText(
                this,
                "Nombre y especie son obligatorios",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val edad = edadStr.toIntOrNull()

        CoroutineScope(Dispatchers.IO).launch {
            try {

                // 1.OBTENER EMAIL DE SESIÓN
                val email = SessionManager(this@AgregarMascotaActivity).getEmail()

                if (email == null) {
                    runOnUiThread {
                        Toast.makeText(
                            this@AgregarMascotaActivity,
                            "Sesión no válida",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                // 2.OBTENER prop_id
                val propResponse = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/propietarios"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(
                            HttpHeaders.Authorization,
                            "Bearer ${SupabaseConfig.API_KEY}"
                        )
                    }
                    parameter("prop_email", "eq.$email")
                    parameter("select", "prop_id")
                }

                val propBody = propResponse.bodyAsText()
                val jsonArray = JSONArray(propBody)

                if (jsonArray.length() == 0) {
                    runOnUiThread {
                        Toast.makeText(
                            this@AgregarMascotaActivity,
                            "Propietario no encontrado",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                val propId = jsonArray.getJSONObject(0).getInt("prop_id")

                // 3.INSERTAR MASCOTA
                HttpClientProvider.client.post(
                    "${SupabaseConfig.URL}/rest/v1/pacientes"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(
                            HttpHeaders.Authorization,
                            "Bearer ${SupabaseConfig.API_KEY}"
                        )
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                        append("Prefer", "return=minimal")
                    }

                    setBody(
                        """
                        {
                          "pac_nombre": "$nombre",
                          "pac_especie": "$especie",
                          "pac_raza": "$raza",
                          "pac_edad": ${edad ?: "null"},
                          "prop_id": $propId
                        }
                        """.trimIndent()
                    )
                }

                runOnUiThread {
                    Toast.makeText(
                        this@AgregarMascotaActivity,
                        "Mascota agregada 🐾",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@AgregarMascotaActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
