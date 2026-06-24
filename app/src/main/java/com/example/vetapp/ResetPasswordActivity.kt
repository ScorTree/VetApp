package com.example.vetapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var etNuevaPassword: EditText
    private lateinit var etConfirmarPassword: EditText
    private lateinit var btnActualizarPassword: Button

    private var accessToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        etNuevaPassword = findViewById(R.id.etNuevaPassword)
        etConfirmarPassword = findViewById(R.id.etConfirmarPassword)
        btnActualizarPassword = findViewById(R.id.btnActualizarPassword)

        accessToken = obtenerAccessToken(intent)

        if (accessToken == null) {
            Toast.makeText(
                this,
                "Enlace inválido o expirado",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        btnActualizarPassword.setOnClickListener {
            validarYActualizar()
        }
    }

    private fun obtenerAccessToken(intent: Intent): String? {
        val uri: Uri = intent.data ?: return null

        val fragment = uri.fragment ?: return null

        val partes = fragment.split("&")

        for (parte in partes) {
            val keyValue = parte.split("=")

            if (keyValue.size == 2 && keyValue[0] == "access_token") {
                return keyValue[1]
            }
        }

        return null
    }

    private fun validarYActualizar() {
        val nueva = etNuevaPassword.text.toString().trim()
        val confirmar = etConfirmarPassword.text.toString().trim()

        if (nueva.isEmpty() || confirmar.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (nueva.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        if (nueva != confirmar) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        actualizarPassword(nueva)
    }

    private fun actualizarPassword(nuevaPassword: String) {
        btnActualizarPassword.isEnabled = false
        btnActualizarPassword.text = "Actualizando..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                HttpClientProvider.client.put(
                    "${SupabaseConfig.URL}/auth/v1/user"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer $accessToken")
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                    }

                    setBody(
                        JSONObject().apply {
                            put("password", nuevaPassword)
                        }.toString()
                    )
                }

                runOnUiThread {
                    Toast.makeText(
                        this@ResetPasswordActivity,
                        "Contraseña actualizada correctamente",
                        Toast.LENGTH_LONG
                    ).show()

                    startActivity(
                        Intent(
                            this@ResetPasswordActivity,
                            LoginActivity::class.java
                        )
                    )

                    finish()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    btnActualizarPassword.isEnabled = true
                    btnActualizarPassword.text = "Actualizar contraseña"

                    Toast.makeText(
                        this@ResetPasswordActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}