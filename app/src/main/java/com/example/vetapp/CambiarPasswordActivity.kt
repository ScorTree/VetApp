package com.example.vetapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.util.Patterns
import android.util.Log
import android.view.MotionEvent
import android.view.View
import io.ktor.client.statement.bodyAsText
import org.json.JSONArray
import androidx.appcompat.app.AppCompatActivity
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class CambiarPasswordActivity : AppCompatActivity() {

    private lateinit var etCorreo: EditText
    private lateinit var btnCambiarPassword: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cambiar_password)

        etCorreo = findViewById(R.id.etCorreo)
        btnCambiarPassword = findViewById(R.id.btnCambiarPassword)

        btnCambiarPassword.setOnClickListener {
            enviarCorreoRecuperacion()
        }
    }

    private fun enviarCorreoRecuperacion() {
        val correo = etCorreo.text.toString().trim()

        etCorreo.setText(correo)
        etCorreo.setSelection(correo.length)

        if (correo.isEmpty()) {
            Toast.makeText(
                this,
                "Ingresa tu correo electrónico",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(
                this,
                "Ingresa un correo electrónico válido",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        btnCambiarPassword.isEnabled = false
        btnCambiarPassword.text = "Verificando..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val buscarResponse = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/propietarios"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(
                            HttpHeaders.Authorization,
                            "Bearer ${SupabaseConfig.API_KEY}"
                        )
                    }

                    parameter("prop_email", "eq.$correo")
                    parameter("select", "prop_id")
                }

                val existeCorreo = JSONArray(
                    buscarResponse.bodyAsText()
                ).length() > 0

                if (!existeCorreo) {
                    runOnUiThread {
                        btnCambiarPassword.isEnabled = true
                        btnCambiarPassword.text = "Enviar enlace"

                        Toast.makeText(
                            this@CambiarPasswordActivity,
                            "No existe una cuenta registrada con ese correo.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                btnCambiarPassword.text = "Enviando..."

                HttpClientProvider.client.post(
                    "${SupabaseConfig.URL}/auth/v1/recover"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                    }

                    parameter("redirect_to", "vetapp://reset-password")

                    setBody(
                        JSONObject().apply {
                            put("email", correo)
                        }.toString()
                    )
                }

                runOnUiThread {
                    btnCambiarPassword.isEnabled = true
                    btnCambiarPassword.text = "Enviar enlace"

                    Toast.makeText(
                        this@CambiarPasswordActivity,
                        "Se envió un enlace de recuperación a tu correo.",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                }

            } catch (e: Exception) {
                Log.e("RESET_PASSWORD", "Error al enviar correo de recuperación", e)

                runOnUiThread {
                    btnCambiarPassword.isEnabled = true
                    btnCambiarPassword.text = "Enviar enlace"

                    Toast.makeText(
                        this@CambiarPasswordActivity,
                        "Comprueba tu conexión a Internet e inténtalo nuevamente.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}