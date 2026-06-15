package com.example.vetapp

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

class EditarMascotaActivity : AppCompatActivity() {

    private var pacId: Int = -1

    private lateinit var etNombre: EditText
    private lateinit var etEspecie: EditText
    private lateinit var etRaza: EditText
    private lateinit var etEdad: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_mascota)

        // Obtener pacId
        pacId = intent.getIntExtra("pacId", -1)

        if (pacId == -1) {
            Toast.makeText(this, "Mascota inválida", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Views
        etNombre = findViewById(R.id.etNombre)
        etEspecie = findViewById(R.id.etEspecie)
        etRaza = findViewById(R.id.etRaza)
        etEdad = findViewById(R.id.etEdad)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnGuardar = findViewById<MaterialButton>(R.id.btnGuardar)

        // Regresar SIN guardar
        btnBack.setOnClickListener {
            finish()
        }

        // Guardar cambios
        btnGuardar.setOnClickListener {
            guardarCambios()
        }

        // Cargar datos actuales
        cargarMascota()
    }

    private fun cargarMascota() {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/pacientes"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("pac_id", "eq.$pacId")
                    parameter(
                        "select",
                        "pac_nombre,pac_especie,pac_raza,pac_edad"
                    )
                }

                val array = JSONArray(response.bodyAsText())
                if (array.length() == 0) return@launch

                val obj = array.getJSONObject(0)

                runOnUiThread {
                    etNombre.setText(obj.getString("pac_nombre"))
                    etEspecie.setText(obj.getString("pac_especie"))
                    etRaza.setText(obj.optString("pac_raza", ""))
                    etEdad.setText(
                        if (obj.isNull("pac_edad")) "" else obj.getInt("pac_edad").toString()
                    )
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@EditarMascotaActivity,
                        "Error al cargar datos",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun guardarCambios() {

        val nombre = etNombre.text.toString().trim()
        val especie = etEspecie.text.toString().trim()
        val raza = etRaza.text.toString().trim()
        val edad = etEdad.text.toString().toIntOrNull()

        if (nombre.isEmpty() || especie.isEmpty()) {
            Toast.makeText(
                this,
                "Nombre y especie son obligatorios",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                HttpClientProvider.client.patch(
                    "${SupabaseConfig.URL}/rest/v1/pacientes"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                    }
                    parameter("pac_id", "eq.$pacId")

                    setBody(
                        """
                        {
                          "pac_nombre": "$nombre",
                          "pac_especie": "$especie",
                          "pac_raza": ${if (raza.isEmpty()) "null" else "\"$raza\""},
                          "pac_edad": ${edad ?: "null"}
                        }
                        """.trimIndent()
                    )
                }

                runOnUiThread {
                    Toast.makeText(
                        this@EditarMascotaActivity,
                        "Mascota actualizada 🐾",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish() // 🔙 vuelve a DetalleMascota
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@EditarMascotaActivity,
                        "Error al guardar cambios",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
