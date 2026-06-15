package com.example.vetapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import com.example.vetapp.util.PasswordUtils
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etTelefono = findViewById<EditText>(R.id.etTelefono)
        val etDireccion = findViewById<EditText>(R.id.etDireccion)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        btnRegister.setOnClickListener {
            register(
                etNombre.text.toString().trim(),
                etEmail.text.toString().trim(),
                etPassword.text.toString().trim(),
                etTelefono.text.toString().trim(),
                etDireccion.text.toString().trim()
            )
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun register(
        nombre: String,
        email: String,
        password: String,
        telefono: String,
        direccion: String
    ) {

        if (
            nombre.isEmpty() ||
            email.isEmpty() ||
            password.isEmpty() ||
            telefono.isEmpty()
        ) {
            Toast.makeText(
                this,
                "Completa todos los campos",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {

                // HASH DE CONTRASEÑA
                val passwordHash = PasswordUtils.hashPassword(password)

                // 1.CREAR USUARIO EN SUPABASE AUTH
                HttpClientProvider.client.post(
                    "${SupabaseConfig.URL}/auth/v1/signup"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                    }
                    setBody(
                        """
                        {
                          "email": "$email",
                          "password": "$password"
                        }
                        """.trimIndent()
                    )
                }

                // 2.INSERTAR EN TABLA PROPIETARIOS
                HttpClientProvider.client.post(
                    "${SupabaseConfig.URL}/rest/v1/propietarios"
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
                          "prop_email": "$email",
                          "prop_pass": "$passwordHash",
                          "prop_nombre": "$nombre",
                          "prop_tel": "$telefono",
                          "prop_dir": "$direccion"
                        }
                        """.trimIndent()
                    )
                }

                runOnUiThread {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Registro exitoso 🎉",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(
                        Intent(this@RegisterActivity, LoginActivity::class.java)
                    )
                    finish()
                }

            } catch (e: io.ktor.client.plugins.ClientRequestException) {

                val error = e.response.bodyAsText()

                runOnUiThread {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Error de registro\n$error",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                runOnUiThread {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
