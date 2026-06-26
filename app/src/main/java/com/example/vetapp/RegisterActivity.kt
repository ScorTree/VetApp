package com.example.vetapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import android.text.InputType
import android.view.MotionEvent
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import com.example.vetapp.util.PasswordUtils
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmarPassword = findViewById<EditText>(R.id.etConfirmarPassword)
        val etTelefono = findViewById<EditText>(R.id.etTelefono)
        val etDireccion = findViewById<EditText>(R.id.etDireccion)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        val layoutPasswordRules = findViewById<LinearLayout>(R.id.layoutPasswordRules)
        val tvPasswordStrength = findViewById<TextView>(R.id.tvPasswordStrength)
        val tvPasswordBar = findViewById<TextView>(R.id.tvPasswordBar)
        val tvRuleLength = findViewById<TextView>(R.id.tvRuleLength)
        val tvRuleUpper = findViewById<TextView>(R.id.tvRuleUpper)
        val tvRuleLower = findViewById<TextView>(R.id.tvRuleLower)
        val tvRuleNumber = findViewById<TextView>(R.id.tvRuleNumber)
        val tvRuleSpecial = findViewById<TextView>(R.id.tvRuleSpecial)

        configurarMostrarPassword(etPassword)
        configurarMostrarPassword(etConfirmarPassword)

        etPassword.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                layoutPasswordRules.visibility = View.VISIBLE

                actualizarVistaPassword(
                    password = s.toString(),
                    tvPasswordStrength = tvPasswordStrength,
                    tvPasswordBar = tvPasswordBar,
                    tvRuleLength = tvRuleLength,
                    tvRuleUpper = tvRuleUpper,
                    tvRuleLower = tvRuleLower,
                    tvRuleNumber = tvRuleNumber,
                    tvRuleSpecial = tvRuleSpecial
                )
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        btnRegister.setOnClickListener {

            val nombreLimpio = formatearNombre(
                etNombre.text.toString()
            )

            val direccionLimpia = formatearDireccion(
                etDireccion.text.toString()
            )

            etNombre.setText(nombreLimpio)
            etNombre.setSelection(nombreLimpio.length)

            etDireccion.setText(direccionLimpia)
            etDireccion.setSelection(direccionLimpia.length)

            register(
                nombre = nombreLimpio,
                email = etEmail.text.toString().trim(),
                password = etPassword.text.toString().trim(),
                confirmarPassword = etConfirmarPassword.text.toString().trim(),
                telefono = etTelefono.text.toString().trim(),
                direccion = direccionLimpia,
                btnRegister = btnRegister
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
        confirmarPassword: String,
        telefono: String,
        direccion: String,
        btnRegister: Button
    ) {
        val nombreLimpio = capitalizarTexto(limpiarTexto(nombre))
        val direccionLimpia = capitalizarTexto(limpiarTexto(direccion))

        if (nombreLimpio.isEmpty() || email.isEmpty() || password.isEmpty() || telefono.isEmpty()) {
            Toast.makeText(this, "Completa los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        if (!nombreValido(nombreLimpio)) {
            Toast.makeText(
                this,
                "Ingresa nombre y apellido válidos",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Ingresa un correo electrónico válido", Toast.LENGTH_LONG).show()
            return
        }

        if (!passwordValida(password)) {
            Toast.makeText(
                this,
                "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (password != confirmarPassword) {
            Toast.makeText(
                this,
                "Las contraseñas no coinciden",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!telefono.matches(Regex("^\\d{10}$"))) {
            Toast.makeText(this, "El teléfono debe tener exactamente 10 dígitos", Toast.LENGTH_LONG).show()
            return
        }

        if (direccionLimpia.isNotEmpty() && !direccionValida(direccion)) {
            Toast.makeText(this, "Ingresa una dirección válida o deja el campo vacío", Toast.LENGTH_LONG).show()
            return
        }

        val telefonoCompleto = telefono

        btnRegister.isEnabled = false
        btnRegister.text = "Registrando..."

        CoroutineScope(Dispatchers.IO).launch {
            try {

                // Verificar correo duplicado en propietarios
                val correoRes = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/propietarios"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("prop_email", "eq.$email")
                    parameter("select", "prop_id")
                }

                if (JSONArray(correoRes.bodyAsText()).length() > 0) {
                    runOnUiThread {

                        btnRegister.isEnabled = true
                        btnRegister.text = "Registrarse"

                        Toast.makeText(
                            this@RegisterActivity,
                            "Ya existe una cuenta con ese correo",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                // Verificar teléfono duplicado
                val telRes = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/propietarios"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("prop_tel", "eq.$telefonoCompleto")
                    parameter("select", "prop_id")
                }

                if (JSONArray(telRes.bodyAsText()).length() > 0) {
                    runOnUiThread {

                        btnRegister.isEnabled = true
                        btnRegister.text = "Registrarse"

                        Toast.makeText(
                            this@RegisterActivity,
                            "Ese número telefónico ya está registrado",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                val passwordHash = PasswordUtils.hashPassword(password)

                // Crear usuario en Supabase Auth
                val authResponse = HttpClientProvider.client.post(
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

                val authBody = authResponse.bodyAsText()

                if (authBody.contains("already registered", ignoreCase = true) ||
                    authBody.contains("User already registered", ignoreCase = true)
                ) {
                    runOnUiThread {

                        btnRegister.isEnabled = true
                        btnRegister.text = "Registrarse"

                        Toast.makeText(
                            this@RegisterActivity,
                            "Ya existe una cuenta con ese correo",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                // Insertar propietario
                HttpClientProvider.client.post(
                    "${SupabaseConfig.URL}/rest/v1/propietarios"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                        append("Prefer", "return=minimal")
                    }
                    setBody(
                        """
                        {
                          "prop_email": "$email",
                          "prop_pass": "$passwordHash",
                          "prop_nombre": "$nombreLimpio",
                          "prop_tel": "$telefonoCompleto",
                          "prop_dir": "$direccionLimpia"
                        }
                        """.trimIndent()
                    )
                }

                runOnUiThread {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Registro exitoso.",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                }

            } catch (e: ClientRequestException) {

                val error = e.response.bodyAsText()

                runOnUiThread {

                    btnRegister.isEnabled = true
                    btnRegister.text = "Registrarse"

                    val mensaje = when {
                        error.contains("already", ignoreCase = true) ||
                                error.contains("registered", ignoreCase = true) ->
                            "Ya existe una cuenta con ese correo"

                        else -> "Error de registro"
                    }

                    Toast.makeText(
                        this@RegisterActivity,
                        mensaje,
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                android.util.Log.e("REGISTER", "Error durante el registro", e)

                runOnUiThread {

                    btnRegister.isEnabled = true
                    btnRegister.text = "Registrarse"

                    Toast.makeText(
                        this@RegisterActivity,
                        "Comprueba tu conexión a Internet e inténtalo nuevamente.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun passwordValida(password: String): Boolean {
        return Regex(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=!?.*_-]).{8,}$"
        ).matches(password)
    }

    private fun direccionValida(direccion: String): Boolean {

        val texto = direccion.trim()

        if (texto.length < 10) return false

        if (!texto.any { it.isLetter() }) return false

        if (!texto.any { it.isDigit() }) return false

        val palabras = texto.split(Regex("\\s+"))

        if (palabras.size < 2) return false

        if (texto.matches(Regex("^\\d+$"))) return false

        if (texto.matches(Regex("^[^A-Za-zÁÉÍÓÚáéíóúÑñ0-9]+$"))) return false

        return true
    }

    private fun tieneTresLetrasIgualesSeguidas(texto: String): Boolean {
        return Regex("(.)\\1\\1").containsMatchIn(texto.lowercase())
    }

    private fun nombreValido(nombre: String): Boolean {

        val limpio = nombre.trim().replace(Regex("\\s+"), " ")

        val partes = limpio.split(" ")

        if (partes.size < 2) return false

        for (parte in partes) {

            if (parte.length < 2) return false

            if (!parte.matches(Regex("^[A-Za-zÁÉÍÓÚáéíóúÑñ]+$"))) {
                return false
            }

            if (tieneTresLetrasIgualesSeguidas(parte)) {
                return false
            }
        }

        return true
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

    private fun actualizarVistaPassword(
        password: String,
        tvPasswordStrength: TextView,
        tvPasswordBar: TextView,
        tvRuleLength: TextView,
        tvRuleUpper: TextView,
        tvRuleLower: TextView,
        tvRuleNumber: TextView,
        tvRuleSpecial: TextView
    ) {
        val rojo = Color.parseColor("#D32F2F")
        val naranja = Color.parseColor("#F57C00")
        val amarillo = Color.parseColor("#FBC02D")
        val verde = Color.parseColor("#2E7D32")

        val cumpleLongitud = password.length >= 8
        val cumpleMayuscula = password.any { it.isUpperCase() }
        val cumpleMinuscula = password.any { it.isLowerCase() }
        val cumpleNumero = password.any { it.isDigit() }
        val cumpleEspecial = password.any { !it.isLetterOrDigit() }

        tvRuleLength.setTextColor(if (cumpleLongitud) verde else rojo)
        tvRuleUpper.setTextColor(if (cumpleMayuscula) verde else rojo)
        tvRuleLower.setTextColor(if (cumpleMinuscula) verde else rojo)
        tvRuleNumber.setTextColor(if (cumpleNumero) verde else rojo)
        tvRuleSpecial.setTextColor(if (cumpleEspecial) verde else rojo)

        val puntos = listOf(
            cumpleLongitud,
            cumpleMayuscula,
            cumpleMinuscula,
            cumpleNumero,
            cumpleEspecial
        ).count { it }

        when (puntos) {
            0, 1 -> {

                tvPasswordStrength.setTextColor(Color.parseColor("#8E7CC3"))

                tvPasswordBar.visibility = View.VISIBLE
                tvRuleLength.visibility = View.VISIBLE
                tvRuleUpper.visibility = View.VISIBLE
                tvRuleLower.visibility = View.VISIBLE
                tvRuleNumber.visibility = View.VISIBLE
                tvRuleSpecial.visibility = View.VISIBLE

                tvPasswordStrength.text = "Fortaleza: Muy débil"
                tvPasswordBar.text = "██░░░░░░░░"
                tvPasswordBar.setTextColor(rojo)
            }

            2 -> {

                tvPasswordStrength.setTextColor(Color.parseColor("#8E7CC3"))

                tvPasswordBar.visibility = View.VISIBLE
                tvRuleLength.visibility = View.VISIBLE
                tvRuleUpper.visibility = View.VISIBLE
                tvRuleLower.visibility = View.VISIBLE
                tvRuleNumber.visibility = View.VISIBLE
                tvRuleSpecial.visibility = View.VISIBLE

                tvPasswordStrength.text = "Fortaleza: Débil"
                tvPasswordBar.text = "████░░░░░░"
                tvPasswordBar.setTextColor(naranja)
            }

            3, 4 -> {

                tvPasswordStrength.setTextColor(Color.parseColor("#8E7CC3"))

                tvPasswordBar.visibility = View.VISIBLE
                tvRuleLength.visibility = View.VISIBLE
                tvRuleUpper.visibility = View.VISIBLE
                tvRuleLower.visibility = View.VISIBLE
                tvRuleNumber.visibility = View.VISIBLE
                tvRuleSpecial.visibility = View.VISIBLE

                tvPasswordStrength.text = "Fortaleza: Media"
                tvPasswordBar.text = "███████░░░"
                tvPasswordBar.setTextColor(amarillo)
            }

            5 -> {
                tvPasswordStrength.text = "🟢 Contraseña segura"
                tvPasswordStrength.setTextColor(verde)

                tvPasswordBar.visibility = View.GONE
                tvRuleLength.visibility = View.GONE
                tvRuleUpper.visibility = View.GONE
                tvRuleLower.visibility = View.GONE
                tvRuleNumber.visibility = View.GONE
                tvRuleSpecial.visibility = View.GONE
            }
        }
    }

    private fun capitalizarTexto(texto: String): String {
        return texto
            .lowercase()
            .split(" ")
            .joinToString(" ") { palabra ->
                palabra.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase()
                    else char.toString()
                }
            }
    }

    private fun limpiarTexto(texto: String): String {
        return texto
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun formatearNombre(nombre: String): String {

        return limpiarTexto(nombre)
            .lowercase()
            .split(" ")
            .joinToString(" ") { palabra ->

                palabra.replaceFirstChar {

                    if (it.isLowerCase())
                        it.titlecase()
                    else
                        it.toString()

                }

            }
    }

    private fun formatearDireccion(direccion: String): String {

        val excepciones = listOf(
            "de",
            "del",
            "la",
            "las",
            "los",
            "y",
            "e"
        )

        return limpiarTexto(direccion)
            .lowercase()
            .split(" ")
            .joinToString(" ") { palabra ->

                if (palabra in excepciones) {

                    palabra

                } else {

                    palabra.replaceFirstChar {

                        if (it.isLowerCase())
                            it.titlecase()
                        else
                            it.toString()

                    }

                }

            }
    }
}