package com.example.vetapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import com.example.vetapp.util.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class AgendaVetActivity : AppCompatActivity() {

    private lateinit var rvCitas: RecyclerView
    private lateinit var tvVacia: TextView
    private lateinit var tvResultados: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var etBuscar: TextInputEditText
    private val todasLasCitas = mutableListOf<CitaVet>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agenda_vet)

        rvCitas      = findViewById(R.id.rvCitasVet)
        tvVacia      = findViewById(R.id.tvAgendaVacia)
        tvResultados = findViewById(R.id.tvResultados)
        tabLayout    = findViewById(R.id.tabsFiltro)
        etBuscar     = findViewById(R.id.etBuscarCita)

        rvCitas.layoutManager = LinearLayoutManager(this)

        // Bottom nav
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.vet_menu_agenda
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.vet_menu_inicio -> {
                    startActivity(Intent(this, VetDashboardActivity::class.java))
                    overridePendingTransition(0, 0); finish(); true
                }
                R.id.vet_menu_agenda -> true
                R.id.vet_menu_inventario -> {
                    startActivity(Intent(this, InventarioActivity::class.java))
                    overridePendingTransition(0, 0); finish(); true
                }
                R.id.vet_menu_perfil -> {
                    startActivity(Intent(this, PerfilVetActivity::class.java))
                    overridePendingTransition(0, 0); finish(); true
                }
                else -> false
            }
        }

        // Tabs
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { aplicarFiltros() }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Buscador
        etBuscar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) { aplicarFiltros() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        cargarCitas()
    }

    override fun onResume() {
        super.onResume()
        cargarCitas()
    }

    private fun cargarCitas() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/citas"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("select",
                        "cita_id,cita_fecha,cita_hora,cita_motivo,cita_estado,pac_id," +
                                "pacientes(pac_nombre,pac_especie,pac_raza,pac_edad," +
                                "propietarios(prop_nombre))")
                    parameter("order", "cita_fecha.asc,cita_hora.asc")
                }

                val arr = JSONArray(res.bodyAsText())
                todasLasCitas.clear()

                for (i in 0 until arr.length()) {
                    val o   = arr.getJSONObject(i)
                    val pac = o.optJSONObject("pacientes")
                    val prop = pac?.optJSONObject("propietarios")
                    todasLasCitas.add(
                        CitaVet(
                            citaId     = o.getInt("cita_id"),
                            fecha      = o.getString("cita_fecha"),
                            hora       = o.getString("cita_hora").substring(0, 5),
                            motivo     = o.getString("cita_motivo"),
                            estado     = o.optString("cita_estado", "pendiente"),
                            pacId      = o.getInt("pac_id"),
                            pacNombre  = pac?.optString("pac_nombre", "Desconocido") ?: "Desconocido",
                            pacEspecie = pac?.optString("pac_especie", "") ?: "",
                            pacRaza    = pac?.optString("pac_raza", "") ?: "",
                            pacEdad    = if (pac?.isNull("pac_edad") == true) null
                            else pac?.optInt("pac_edad"),
                            propNombre = prop?.optString("prop_nombre", "") ?: ""
                        )
                    )
                }

                runOnUiThread { aplicarFiltros() }

            } catch (e: Exception) {
                runOnUiThread {
                    tvVacia.visibility = View.VISIBLE
                    tvVacia.text = "Error al cargar citas"
                }
            }
        }
    }

    private fun aplicarFiltros() {
        val tabPos = tabLayout.selectedTabPosition
        val query  = etBuscar.text.toString().trim()

        var filtradas = when (tabPos) {
            1    -> todasLasCitas.filter { it.estado == "confirmada" }
            2    -> todasLasCitas.filter { it.estado == "pendiente" }
            3    -> todasLasCitas.filter { it.estado == "cancelada" }
            4    -> todasLasCitas.filter { it.estado == "completada" }
            else -> todasLasCitas.toList()
        }

        if (query.isNotEmpty()) {
            filtradas = filtradas.filter {
                it.pacNombre.contains(query, ignoreCase = true) ||
                        it.propNombre.contains(query, ignoreCase = true)
            }
        }

        tvResultados.text = "Resultados (${filtradas.size})"

        if (filtradas.isEmpty()) {
            tvVacia.visibility = View.VISIBLE
            rvCitas.visibility = View.GONE
        } else {
            tvVacia.visibility = View.GONE
            rvCitas.visibility = View.VISIBLE
            rvCitas.adapter = CitaVetAdapter(filtradas) { cita ->
                val intent = Intent(this, DetalleCitaVetActivity::class.java)
                intent.putExtra("citaId",    cita.citaId)
                intent.putExtra("pacId",     cita.pacId)
                intent.putExtra("pacNombre", cita.pacNombre)
                intent.putExtra("motivo",    cita.motivo)
                intent.putExtra("estado",    cita.estado)
                startActivity(intent)
            }
        }
    }
}