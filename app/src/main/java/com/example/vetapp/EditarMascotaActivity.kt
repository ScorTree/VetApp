package com.example.vetapp

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import android.util.Log
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

        val nombre = formatearTextoMascota(etNombre.text.toString())
        val especie = formatearTextoMascota(etEspecie.text.toString())
        val raza = formatearTextoMascota(etRaza.text.toString())
        val edadStr = etEdad.text.toString().trim()

        etNombre.setText(nombre)
        etNombre.setSelection(nombre.length)

        etEspecie.setText(especie)
        etEspecie.setSelection(especie.length)

        etRaza.setText(raza)
        etRaza.setSelection(raza.length)

        if (!nombreMascotaValido(nombre)) {
            Toast.makeText(
                this,
                "Ingresa un nombre de mascota válido",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!especieValida(especie)) {
            Toast.makeText(
                this,
                "Ingresa una especie válida",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (raza.isNotEmpty() && !razaValida(raza)) {
            Toast.makeText(
                this,
                "Ingresa una raza válida o deja el campo vacío",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (edadStr.isEmpty()) {
            Toast.makeText(
                this,
                "Ingresa la edad de la mascota",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val edad = edadStr.toIntOrNull()

        if (edad == null || edad !in 0..30) {
            Toast.makeText(
                this,
                "La edad debe estar entre 0 y 30 años",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val btnGuardar = findViewById<MaterialButton>(R.id.btnGuardar)
        btnGuardar.isEnabled = false
        btnGuardar.text = "Guardando..."

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
                      "pac_edad": $edad
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
                    finish()
                }

            } catch (e: Exception) {

                Log.e("EDITAR_MASCOTA", "Error al guardar cambios", e)

                runOnUiThread {
                    btnGuardar.isEnabled = true
                    btnGuardar.text = "Guardar cambios"

                    Toast.makeText(
                        this@EditarMascotaActivity,
                        "Comprueba tu conexión a internet e inténtalo nuevamente.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun nombreMascotaValido(nombre: String): Boolean {
        if (nombre.length !in 2..30) return false
        if (!nombre.any { it.isLetter() }) return false
        if (nombre.matches(Regex("^\\d+$"))) return false

        return nombre.matches(
            Regex("^[A-Za-zÁÉÍÓÚáéíóúÑñ0-9 '\\-]+$")
        )
    }

    private fun especieValida(especie: String): Boolean {
        if (especie.length !in 3..30) return false
        if (!especie.any { it.isLetter() }) return false
        if (especie.matches(Regex("^\\d+$"))) return false

        return especie.matches(
            Regex("^[A-Za-zÁÉÍÓÚáéíóúÑñ ]+$")
        )
    }

    private fun razaValida(raza: String): Boolean {
        if (raza.length !in 2..40) return false
        if (!raza.any { it.isLetter() }) return false
        if (raza.matches(Regex("^\\d+$"))) return false

        return raza.matches(
            Regex("^[A-Za-zÁÉÍÓÚáéíóúÑñ0-9 '\\-]+$")
        )
    }

    private fun limpiarTexto(texto: String): String {
        return texto
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun formatearTextoMascota(texto: String): String {
        return limpiarTexto(texto)
            .lowercase()
            .split(" ")
            .joinToString(" ") { palabra ->
                palabra.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase()
                    else it.toString()
                }
            }
    }
}
