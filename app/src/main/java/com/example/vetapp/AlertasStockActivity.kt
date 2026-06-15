package com.example.vetapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import com.example.vetapp.util.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class AlertasStockActivity : AppCompatActivity() {

    private lateinit var rvAlertas: RecyclerView
    private lateinit var tvVacio: TextView
    private lateinit var tvContador: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alertas_stock)

        rvAlertas   = findViewById(R.id.rvAlertas)
        tvVacio     = findViewById(R.id.tvAlertasVacio)
        tvContador  = findViewById(R.id.tvAlertasContador)

        rvAlertas.layoutManager = LinearLayoutManager(this)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.vet_menu_inventario
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.vet_menu_inicio -> {
                    startActivity(Intent(this, VetDashboardActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
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

        cargarAlertas()
    }

    override fun onResume() {
        super.onResume()
        cargarAlertas()
    }

    private fun cargarAlertas() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/medicamentos"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("med_stock", "lt.5")
                    parameter("med_activo", "eq.true")
                    parameter("select",
                        "med_id,med_nombre,med_lote,med_caducidad,med_precio,med_stock")
                    parameter("order", "med_stock.asc")
                }

                val arr = JSONArray(res.bodyAsText())
                val lista = mutableListOf<MedicamentoInventario>()

                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    lista.add(
                        MedicamentoInventario(
                            medId     = o.getInt("med_id"),
                            nombre    = o.getString("med_nombre"),
                            lote      = o.optString("med_lote", "-"),
                            caducidad = o.optString("med_caducidad", "-"),
                            precio    = o.optDouble("med_precio", 0.0),
                            stock     = o.getInt("med_stock")
                        )
                    )
                }

                runOnUiThread {
                    if (lista.isEmpty()) {
                        tvVacio.visibility    = View.VISIBLE
                        rvAlertas.visibility  = View.GONE
                        tvContador.text       = "✓ Todo el stock está en niveles normales"
                        tvContador.setTextColor(
                            android.graphics.Color.parseColor("#4CAF50"))
                    } else {
                        tvVacio.visibility    = View.GONE
                        rvAlertas.visibility  = View.VISIBLE
                        tvContador.text       = "⚠ ${lista.size} medicamento(s) con stock crítico"
                        tvContador.setTextColor(
                            android.graphics.Color.parseColor("#FF5252"))

                        rvAlertas.adapter = InventarioAdapter(
                            lista,
                            onEditar   = {},
                            onEliminar = {}
                        )
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@AlertasStockActivity,
                        "Error al cargar alertas", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}