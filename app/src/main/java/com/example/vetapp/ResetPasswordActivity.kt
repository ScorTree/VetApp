package com.example.vetapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.graphics.Color
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
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

        val layoutPasswordRules = findViewById<LinearLayout>(R.id.layoutPasswordRules)
        val tvPasswordStrength = findViewById<TextView>(R.id.tvPasswordStrength)
        val tvPasswordBar = findViewById<TextView>(R.id.tvPasswordBar)
        val tvRuleLength = findViewById<TextView>(R.id.tvRuleLength)
        val tvRuleUpper = findViewById<TextView>(R.id.tvRuleUpper)
        val tvRuleLower = findViewById<TextView>(R.id.tvRuleLower)
        val tvRuleNumber = findViewById<TextView>(R.id.tvRuleNumber)
        val tvRuleSpecial = findViewById<TextView>(R.id.tvRuleSpecial)

        configurarMostrarPassword(etNuevaPassword)
        configurarMostrarPassword(etConfirmarPassword)

        etNuevaPassword.addTextChangedListener(object : TextWatcher {

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
                val password = s.toString()

                if (password.isEmpty()) {
                    layoutPasswordRules.visibility = View.GONE
                } else {
                    layoutPasswordRules.visibility = View.VISIBLE

                    actualizarVistaPassword(
                        password = password,
                        tvPasswordStrength = tvPasswordStrength,
                        tvPasswordBar = tvPasswordBar,
                        tvRuleLength = tvRuleLength,
                        tvRuleUpper = tvRuleUpper,
                        tvRuleLower = tvRuleLower,
                        tvRuleNumber = tvRuleNumber,
                        tvRuleSpecial = tvRuleSpecial
                    )
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

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

        if (!passwordValida(nueva)) {
            Toast.makeText(
                this,
                "La contraseña no cumple con los requisitos de seguridad.",
                Toast.LENGTH_LONG
            ).show()
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

                Log.e(
                    "RESET_PASSWORD",
                    "Error al actualizar contraseña",
                    e
                )

                runOnUiThread {

                    btnActualizarPassword.isEnabled = true
                    btnActualizarPassword.text = "Actualizar contraseña"

                    Toast.makeText(
                        this@ResetPasswordActivity,
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