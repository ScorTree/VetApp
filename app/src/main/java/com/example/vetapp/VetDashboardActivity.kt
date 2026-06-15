package com.example.vetapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import com.example.vetapp.util.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class VetDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vet_dashboard)

        val tvNombreVet    = findViewById<TextView>(R.id.tvNombreVet)
        val tvNombrePerfil = findViewById<TextView>(R.id.tvNombrePerfil)
        val btnLogout      = findViewById<ImageButton>(R.id.btnLogout)
        val cardAgenda     = findViewById<MaterialCardView>(R.id.cardAgenda)
        val bottomNav      = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Nombre del veterinario
        val email = SessionManager(this).getEmail() ?: ""
        cargarNombreVet(email, tvNombreVet, tvNombrePerfil)

        // Logout
        btnLogout.setOnClickListener {
            SessionManager(this).logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Card agenda de hoy → va a AgendaVetActivity
        cardAgenda.setOnClickListener {
            startActivity(Intent(this, AgendaVetActivity::class.java))
            overridePendingTransition(0, 0)
        }

        // Bottom nav
        bottomNav.selectedItemId = R.id.vet_menu_inicio
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.vet_menu_inicio -> true

                R.id.vet_menu_agenda -> {
                    startActivity(Intent(this, AgendaVetActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                R.id.vet_menu_inventario -> {
                    startActivity(Intent(this, InventarioActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                R.id.vet_menu_perfil -> {
                    startActivity(Intent(this, PerfilVetActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                else -> false
            }
        }

        cargarResumen()
    }

    override fun onResume() {
        super.onResume()
        cargarResumen()
    }

    private fun cargarNombreVet(
        email: String,
        tvNombreVet: TextView,
        tvNombrePerfil: TextView
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/veterinarios"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("vet_email", "eq.$email")
                    parameter("select", "vet_nombre")
                }
                val arr = JSONArray(res.bodyAsText())
                if (arr.length() > 0) {
                    val nombre = arr.getJSONObject(0).getString("vet_nombre")
                    runOnUiThread {
                        tvNombreVet.text = "Hola, $nombre"
                        tvNombrePerfil.text = "Nombre: $nombre"
                    }
                }
            } catch (e: Exception) {
                // silencioso
            }
        }
    }

    private fun cargarResumen() {
        val tvCitasHoy    = findViewById<TextView>(R.id.tvCitasHoy)
        val tvPendientes  = findViewById<TextView>(R.id.tvPendientes)
        val tvCompletadas = findViewById<TextView>(R.id.tvCompletadas)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/citas"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("select", "cita_id,cita_estado")
                }

                val arr = JSONArray(res.bodyAsText())
                var pendientes  = 0
                var completadas = 0

                for (i in 0 until arr.length()) {
                    when (arr.getJSONObject(i).optString("cita_estado", "pendiente")) {
                        "pendiente"  -> pendientes++
                        "confirmada",
                        "pagada",
                        "completada" -> completadas++
                    }
                }

                runOnUiThread {
                    tvCitasHoy.text    = "${arr.length()} Citas"
                    tvPendientes.text  = "$pendientes Citas"
                    tvCompletadas.text = "$completadas Citas"
                }

            } catch (e: Exception) {
                runOnUiThread {
                    tvCitasHoy.text    = "0 Citas"
                    tvPendientes.text  = "0 Citas"
                    tvCompletadas.text = "0 Citas"
                }
            }
        }
    }
}