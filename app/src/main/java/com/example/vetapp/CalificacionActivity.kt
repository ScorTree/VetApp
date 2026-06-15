package com.example.vetapp

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import com.example.vetapp.util.SessionManager
import com.google.android.material.button.MaterialButton
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class CalificacionActivity : AppCompatActivity() {

    private lateinit var star1: ImageView
    private lateinit var star2: ImageView
    private lateinit var star3: ImageView
    private lateinit var star4: ImageView
    private lateinit var star5: ImageView

    private lateinit var tvMensaje: TextView
    private lateinit var etComentario: EditText
    private lateinit var btnEnviar: MaterialButton

    private var calificacion = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calificacion)

        // Flecha regresar
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Estrellas
        star1 = findViewById(R.id.star1)
        star2 = findViewById(R.id.star2)
        star3 = findViewById(R.id.star3)
        star4 = findViewById(R.id.star4)
        star5 = findViewById(R.id.star5)

        // Controles
        tvMensaje = findViewById(R.id.tvMensaje)
        etComentario = findViewById(R.id.etComentario)
        btnEnviar = findViewById(R.id.btnEnviar)

        // Eventos de las estrellas
        star1.setOnClickListener { actualizarEstrellas(1) }
        star2.setOnClickListener { actualizarEstrellas(2) }
        star3.setOnClickListener { actualizarEstrellas(3) }
        star4.setOnClickListener { actualizarEstrellas(4) }
        star5.setOnClickListener { actualizarEstrellas(5) }

        // Cargar calificación anterior (si existe)
        cargarCalificacionExistente()

        btnEnviar.setOnClickListener {

            if (calificacion == 0) {

                Toast.makeText(
                    this,
                    "Selecciona una calificación",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            guardarCalificacion()
        }
    }

    private fun actualizarEstrellas(valor: Int) {

        calificacion = valor

        val estrellas = listOf(
            star1,
            star2,
            star3,
            star4,
            star5
        )

        estrellas.forEachIndexed { index, imageView ->

            if (index < valor) {

                imageView.setImageResource(
                    R.drawable.ic_star_filled
                )

            } else {

                imageView.setImageResource(
                    R.drawable.ic_star_empty
                )

            }

        }

        tvMensaje.text = when (valor) {

            1 -> "😔 Lamentamos que tu experiencia no haya sido la mejor."

            2 -> "😕 Gracias por tu sinceridad, seguiremos mejorando."

            3 -> "🙂 ¡Gracias por compartir tu opinión!"

            4 -> "😊 Nos alegra que hayas tenido una buena experiencia."

            5 -> "🤩 ¡Excelente! Gracias por confiar en VetApp."

            else -> "Selecciona una calificación"
        }
    }

    private fun cargarCalificacionExistente() {

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val email =
                    SessionManager(this@CalificacionActivity).getEmail()
                        ?: return@launch

                // Obtener prop_id

                val propietarioResponse =
                    HttpClientProvider.client.get(
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

                val propietarios =
                    JSONArray(propietarioResponse.bodyAsText())

                if (propietarios.length() == 0)
                    return@launch

                val propId =
                    propietarios.getJSONObject(0)
                        .getInt("prop_id")

                // Buscar calificación existente

                val calificacionResponse =
                    HttpClientProvider.client.get(
                        "${SupabaseConfig.URL}/rest/v1/calificaciones"
                    ) {

                        headers {
                            append("apikey", SupabaseConfig.API_KEY)
                            append(
                                HttpHeaders.Authorization,
                                "Bearer ${SupabaseConfig.API_KEY}"
                            )
                        }

                        parameter("prop_id", "eq.$propId")

                        parameter(
                            "select",
                            "cal_estrellas,cal_comentario"
                        )
                    }

                val array =
                    JSONArray(calificacionResponse.bodyAsText())

                if (array.length() == 0)
                    return@launch

                val obj =
                    array.getJSONObject(0)

                runOnUiThread {

                    calificacion =
                        obj.getInt("cal_estrellas")

                    actualizarEstrellas(calificacion)

                    etComentario.setText(
                        obj.optString(
                            "cal_comentario",
                            ""
                        )
                    )

                    btnEnviar.text =
                        "Actualizar Calificación"
                }

            } catch (_: Exception) {

            }

        }

    }

    private fun guardarCalificacion() {

        btnEnviar.isEnabled = false
        btnEnviar.text = "Enviando..."

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val email = SessionManager(this@CalificacionActivity)
                    .getEmail()

                if (email == null) {

                    runOnUiThread {

                        Toast.makeText(
                            this@CalificacionActivity,
                            "No se encontró la sesión",
                            Toast.LENGTH_LONG
                        ).show()

                        btnEnviar.isEnabled = true
                        btnEnviar.text = "Enviar Calificación"
                    }

                    return@launch
                }

                // ==========================
                // OBTENER PROPIETARIO
                // ==========================

                val propietarioResponse =
                    HttpClientProvider.client.get(
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

                val propietarios =
                    JSONArray(propietarioResponse.bodyAsText())

                if (propietarios.length() == 0) {

                    runOnUiThread {

                        Toast.makeText(
                            this@CalificacionActivity,
                            "No se encontró el propietario",
                            Toast.LENGTH_LONG
                        ).show()

                        btnEnviar.isEnabled = true
                        btnEnviar.text = "Enviar Calificación"
                    }

                    return@launch
                }

                val propId =
                    propietarios.getJSONObject(0)
                        .getInt("prop_id")

                // ==========================
                // VERIFICAR SI YA EXISTE
                // ==========================

                val existeResponse =
                    HttpClientProvider.client.get(
                        "${SupabaseConfig.URL}/rest/v1/calificaciones"
                    ) {

                        headers {

                            append("apikey", SupabaseConfig.API_KEY)

                            append(
                                HttpHeaders.Authorization,
                                "Bearer ${SupabaseConfig.API_KEY}"
                            )

                        }

                        parameter("prop_id", "eq.$propId")
                        parameter("select", "cal_id")
                    }

                val existentes =
                    JSONArray(existeResponse.bodyAsText())

                // ==========================
                // JSON
                // ==========================

                val json = JSONObject()

                json.put(
                    "cal_estrellas",
                    calificacion
                )

                json.put(
                    "cal_comentario",
                    etComentario.text.toString().trim()
                )

                json.put(
                    "cal_actualizado",
                    java.text.SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date())
                )

                // ==========================
                // INSERTAR
                // ==========================

                if (existentes.length() == 0) {

                    json.put(
                        "prop_id",
                        propId
                    )

                    HttpClientProvider.client.post(
                        "${SupabaseConfig.URL}/rest/v1/calificaciones"
                    ) {

                        headers {

                            append(
                                "apikey",
                                SupabaseConfig.API_KEY
                            )

                            append(
                                HttpHeaders.Authorization,
                                "Bearer ${SupabaseConfig.API_KEY}"
                            )

                            append(
                                HttpHeaders.ContentType,
                                "application/json"
                            )

                            append(
                                "Prefer",
                                "return=minimal"
                            )
                        }

                        setBody(json.toString())
                    }

                }

                // ==========================
                // ACTUALIZAR
                // ==========================

                else {

                    HttpClientProvider.client.patch(
                        "${SupabaseConfig.URL}/rest/v1/calificaciones?prop_id=eq.$propId"
                    ) {

                        headers {

                            append(
                                "apikey",
                                SupabaseConfig.API_KEY
                            )

                            append(
                                HttpHeaders.Authorization,
                                "Bearer ${SupabaseConfig.API_KEY}"
                            )

                            append(
                                HttpHeaders.ContentType,
                                "application/json"
                            )

                            append(
                                "Prefer",
                                "return=minimal"
                            )
                        }

                        setBody(json.toString())
                    }

                }

                // ==========================
                // ÉXITO
                // ==========================

                runOnUiThread {

                    Toast.makeText(
                        this@CalificacionActivity,
                        "🐾 ¡Muchas gracias por tu opinión!",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()

                }

            } catch (e: Exception) {

                runOnUiThread {

                    btnEnviar.isEnabled = true

                    btnEnviar.text =
                        if (calificacion == 0)
                            "Enviar Calificación"
                        else
                            "Actualizar Calificación"

                    Toast.makeText(
                        this@CalificacionActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()

                }

            }

        }

    }
}