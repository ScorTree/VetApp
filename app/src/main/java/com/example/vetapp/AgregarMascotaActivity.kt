package com.example.vetapp

import android.os.Bundle
import android.util.Log
import android.widget.*
import android.widget.Spinner
import android.widget.TextView
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

    private lateinit var actEspecie: AutoCompleteTextView
    private lateinit var btnGuardar: Button

    private val especies = listOf(
        "Seleccione una especie",
        "Perro",
        "Gato",
        "Ave",
        "Conejo",
        "Hámster",
        "Pez",
        "Tortuga",
        "Hurón",
        "Otro"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_mascota)

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etRaza = findViewById<EditText>(R.id.etRaza)
        val etEdad = findViewById<EditText>(R.id.etEdad)
        btnGuardar = findViewById(R.id.btnGuardar)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        actEspecie = findViewById(R.id.actEspecie)

        val adapterEspecies = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            especies.drop(1)
        )

        actEspecie.setAdapter(adapterEspecies)

        actEspecie.setOnClickListener {
            actEspecie.showDropDown()
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnGuardar.setOnClickListener {
            val nombreLimpio = formatearTextoMascota(
                etNombre.text.toString()
            )

            val razaLimpia = formatearTextoMascota(
                etRaza.text.toString()
            )

            etNombre.setText(nombreLimpio)
            etNombre.setSelection(nombreLimpio.length)

            etRaza.setText(razaLimpia)
            etRaza.setSelection(razaLimpia.length)

            val especieSeleccionada =
                actEspecie.text.toString().trim()

            guardarMascota(
                nombre = nombreLimpio,
                especie = especieSeleccionada,
                raza = razaLimpia,
                edadStr = etEdad.text.toString().trim()
            )
        }
    }

    private fun guardarMascota(
        nombre: String,
        especie: String,
        raza: String,
        edadStr: String
    ) {

        if (!nombreMascotaValido(nombre)) {
            Toast.makeText(
                this,
                "Ingresa un nombre de mascota válido",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (especie.isEmpty()) {
            Toast.makeText(
                this,
                "Selecciona una especie",
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

        val edad = edadStr.toIntOrNull()

        if (edadStr.isEmpty()) {
            Toast.makeText(
                this,
                "Ingresa la edad de la mascota",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (edad == null || edad !in 0..30) {
            Toast.makeText(
                this,
                "La edad debe estar entre 0 y 30 años",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        btnGuardar.isEnabled = false
        btnGuardar.text = "Guardando..."

        CoroutineScope(Dispatchers.IO).launch {
            try {

                val email = SessionManager(this@AgregarMascotaActivity).getEmail()

                if (email == null) {
                    runOnUiThread {
                        btnGuardar.isEnabled = true
                        btnGuardar.text = "Guardar Mascota"

                        Toast.makeText(
                            this@AgregarMascotaActivity,
                            "Sesión no válida. Inicia sesión nuevamente.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

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
                        btnGuardar.isEnabled = true
                        btnGuardar.text = "Guardar Mascota"

                        Toast.makeText(
                            this@AgregarMascotaActivity,
                            "No se encontró el propietario asociado.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                val propId = jsonArray.getJSONObject(0).getInt("prop_id")

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
                          "pac_edad": $edad,
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

                Log.e("AGREGAR_MASCOTA", "Error al guardar mascota", e)

                runOnUiThread {
                    btnGuardar.isEnabled = true
                    btnGuardar.text = "Guardar Mascota"

                    Toast.makeText(
                        this@AgregarMascotaActivity,
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