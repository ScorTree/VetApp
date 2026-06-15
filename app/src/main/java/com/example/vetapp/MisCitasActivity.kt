package com.example.vetapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import com.example.vetapp.util.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.launch
import org.json.JSONArray

class MisCitasActivity : AppCompatActivity() {

    private lateinit var rvMisCitas: RecyclerView
    private lateinit var adapter: CitaAdapter
    private val listaCitas = mutableListOf<Cita>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_citas)

        rvMisCitas = findViewById(R.id.rvMisCitas)
        rvMisCitas.layoutManager = LinearLayoutManager(this)

        findViewById<ImageButton>(R.id.btnHistorialFinanciero).setOnClickListener {
            startActivity(
                Intent(this, HistorialFinancieroActivity::class.java)
            )
        }

        adapter = CitaAdapter(listaCitas) { cita ->
            confirmarEliminacion(cita)
        }

        rvMisCitas.adapter = adapter

        configurarBottomNav()
        cargarCitas()
    }

    // ===================== BOTTOM NAV =====================
    private fun configurarBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        bottomNav.selectedItemId = R.id.menu_mis_citas

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.menu_inicio -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                R.id.menu_cita -> {
                    startActivity(Intent(this, HacerCitaActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                R.id.menu_mis_citas -> true

                else -> false
            }
        }
    }

    // ===================== CARGAR CITAS =====================
    private fun cargarCitas() {
        lifecycleScope.launch {
            try {
                val email = SessionManager(this@MisCitasActivity)
                    .getEmail() ?: return@launch

                val propRes = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/propietarios"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("prop_email", "eq.$email")
                    parameter("select", "prop_id")
                }

                val propArr = JSONArray(propRes.bodyAsText())
                if (propArr.length() == 0) return@launch
                val propId = propArr.getJSONObject(0).getInt("prop_id")

                val citasRes = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/citas"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter(
                        "select",
                        "cita_id,cita_fecha,cita_hora,cita_motivo,cita_pagada, cita_costo," +
                                "pacientes(pac_nombre,pac_especie,pac_raza,pac_edad)"
                    )
                    parameter("pacientes.prop_id", "eq.$propId")
                    parameter("order", "cita_fecha.asc,cita_hora.asc")
                }

                val arr = JSONArray(citasRes.bodyAsText())
                listaCitas.clear()

                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val p = o.getJSONObject("pacientes")

                    listaCitas.add(
                        Cita(
                            id = o.getInt("cita_id"),
                            mascota = p.getString("pac_nombre"),
                            especie = p.getString("pac_especie"),
                            raza = p.optString("pac_raza", ""),
                            edad = p.optInt("pac_edad", 0),
                            motivo = o.getString("cita_motivo"),
                            fecha = o.getString("cita_fecha"),
                            hora = o.getString("cita_hora"),
                            pagada = o.optBoolean("cita_pagada",false),
                            costo = o.optDouble("cita_costo",350.0)
                        )
                    )
                }

                adapter.notifyDataSetChanged()

            } catch (e: Exception) {
                Log.e("SUPABASE", "MisCitas", e)
                Toast.makeText(
                    this@MisCitasActivity,
                    "Error al cargar citas",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ===================== ELIMINAR =====================
    private fun confirmarEliminacion(cita: Cita) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar cita")
            .setMessage(
                "¿Deseas eliminar esta cita?\n\n" +
                        "Esta acción no se puede deshacer."
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarCita(cita)
            }
            .show()
    }

    private fun eliminarCita(cita: Cita) {
        lifecycleScope.launch {
            try {
                HttpClientProvider.client.delete(
                    "${SupabaseConfig.URL}/rest/v1/citas"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("cita_id", "eq.${cita.id}")
                }

                val index = listaCitas.indexOf(cita)
                if (index != -1) {
                    listaCitas.removeAt(index)
                    adapter.notifyItemRemoved(index)
                }

                Toast.makeText(
                    this@MisCitasActivity,
                    "Cita eliminada",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@MisCitasActivity,
                    "Error al eliminar cita",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
