package com.example.vetapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.text.InputType
import android.util.Patterns
import android.view.MotionEvent
import android.widget.Button
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
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn()) {
            redirigirPorRol(sessionManager.getRol())
            return
        }

        setContentView(R.layout.activity_login)

        findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            startActivity(
                Intent(this, CambiarPasswordActivity::class.java)
            )
        }

        val etEmail    = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin   = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        configurarMostrarPassword(etPassword)

        btnLogin.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            login(
                email,
                password,
                btnLogin
            )
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun login(
        email: String,
        password: String,
        btnLogin: Button
    ) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                this,
                "Completa todos los campos",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(
                this,
                "Ingresa un correo electrónico válido",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        btnLogin.isEnabled = false
        btnLogin.text = "Iniciando..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Autenticar con Supabase Auth
                val response = HttpClientProvider.client.post(
                    "${SupabaseConfig.URL}/auth/v1/token?grant_type=password"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                    }
                    setBody("""{"email":"$email","password":"$password"}""")
                }

                val body = response.bodyAsText()
                val json = JSONObject(body)

                if (!json.has("access_token")) {
                    runOnUiThread {
                        btnLogin.isEnabled = true
                        btnLogin.text = "Iniciar sesión"

                        Toast.makeText(
                            this@LoginActivity,
                            "Correo o contraseña incorrectos",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                val accessToken = json.getString("access_token")

                // 2. Verificar si es veterinario
                val vetRes = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/veterinarios"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("vet_email", "eq.$email")
                    parameter("select", "vet_id")
                }

                val vetArr = JSONArray(vetRes.bodyAsText())
                val rol = if (vetArr.length() > 0) "veterinario" else "propietario"

                // 3. Guardar sesión con rol
                SessionManager(this@LoginActivity).saveSession(accessToken, email, rol)

                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        if (rol == "veterinario") "Bienvenido Doctor 🩺"
                        else "Bienvenido a VetApp 🐾",
                        Toast.LENGTH_SHORT
                    ).show()
                    redirigirPorRol(rol)
                }

            } catch (e: Exception) {

                android.util.Log.e("LOGIN", "Error al iniciar sesión", e)

                runOnUiThread {

                    btnLogin.isEnabled = true
                    btnLogin.text = "Iniciar sesión"

                    Toast.makeText(
                        this@LoginActivity,
                        "Comprueba tu conexión a Internet e inténtalo nuevamente.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun redirigirPorRol(rol: String?) {
        val destino = if (rol == "veterinario") {
            VetDashboardActivity::class.java
        } else {
            MainActivity::class.java
        }
        startActivity(Intent(this, destino))
        finish()
    }

    private fun configurarMostrarPassword(editText: EditText) {

        var visible = false

        editText.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_UP) {

                val drawableEnd = editText.compoundDrawables[2]

                if (drawableEnd != null &&
                    event.rawX >= editText.right - drawableEnd.bounds.width() - editText.paddingEnd
                ) {

                    visible = !visible

                    if (visible) {
                        editText.inputType =
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    } else {
                        editText.inputType =
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    }

                    editText.setSelection(editText.text.length)

                    return@setOnTouchListener true
                }
            }

            false
        }
    }
}