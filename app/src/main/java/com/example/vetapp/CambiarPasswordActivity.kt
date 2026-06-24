package com.example.vetapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
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

        if (correo.isEmpty()) {
            Toast.makeText(this, "Ingresa tu correo electrónico", Toast.LENGTH_SHORT).show()
            return
        }

        btnCambiarPassword.isEnabled = false
        btnCambiarPassword.text = "Enviando..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = HttpClientProvider.client.post(
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

                val body = response.bodyAsText()

                runOnUiThread {
                    btnCambiarPassword.isEnabled = true
                    btnCambiarPassword.text = "Enviar enlace"

                    Toast.makeText(
                        this@CambiarPasswordActivity,
                        "Si el correo está registrado, recibirás un enlace para cambiar tu contraseña.",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    btnCambiarPassword.isEnabled = true
                    btnCambiarPassword.text = "Enviar enlace"

                    Toast.makeText(
                        this@CambiarPasswordActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}