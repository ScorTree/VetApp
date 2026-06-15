package com.example.vetapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import com.example.vetapp.util.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class PerfilVetActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_vet)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.vet_menu_perfil
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.vet_menu_inicio -> {
                    startActivity(Intent(this, VetDashboardActivity::class.java))
                    overridePendingTransition(0, 0); finish(); true
                }
                R.id.vet_menu_agenda -> {
                    startActivity(Intent(this, AgendaVetActivity::class.java))
                    overridePendingTransition(0, 0); finish(); true
                }
                R.id.vet_menu_inventario -> {
                    startActivity(Intent(this, InventarioActivity::class.java))
                    overridePendingTransition(0, 0); finish(); true
                }
                R.id.vet_menu_perfil -> true
                else -> false
            }
        }

        findViewById<MaterialButton>(R.id.btnCerrarSesion).setOnClickListener {
            SessionManager(this).logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        cargarPerfil()
    }

    private fun cargarPerfil() {
        val email = SessionManager(this).getEmail() ?: return
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
                    parameter("select", "vet_nombre,vet_email,vet_tel,vet_especialidad")
                }
                val arr = JSONArray(res.bodyAsText())
                if (arr.length() > 0) {
                    val o = arr.getJSONObject(0)
                    runOnUiThread {
                        findViewById<TextView>(R.id.tvPerfilNombre).text =
                            o.getString("vet_nombre")
                        findViewById<TextView>(R.id.tvPerfilEmail).text =
                            o.getString("vet_email")
                        findViewById<TextView>(R.id.tvPerfilTel).text =
                            o.optString("vet_tel", "-")
                        findViewById<TextView>(R.id.tvPerfilEspecialidad).text =
                            o.optString("vet_especialidad", "-")
                    }
                }
            } catch (_: Exception) {}
        }
    }
}