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
import com.google.android.material.textfield.TextInputEditText
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class PacientesVetActivity : AppCompatActivity() {

    private lateinit var rvPacientes: RecyclerView
    private lateinit var tvVacio: TextView
    private lateinit var etBuscar: TextInputEditText
    private val listaPacientes = mutableListOf<PacienteVet>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pacientes_vet)

        rvPacientes = findViewById(R.id.rvPacientesVet)
        tvVacio     = findViewById(R.id.tvPacientesVacio)
        etBuscar    = findViewById(R.id.etBuscarPaciente)

        rvPacientes.layoutManager = LinearLayoutManager(this)

        // Buscar al escribir
        etBuscar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                filtrarPacientes(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.vet_menu_inicio
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
                    SessionManager(this).logout()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        cargarPacientes()
    }

    override fun onResume() {
        super.onResume()
        cargarPacientes()
    }

    private fun cargarPacientes() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/pacientes"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("select",
                        "pac_id,pac_nombre,pac_especie,pac_raza,pac_edad," +
                                "propietarios(prop_nombre,prop_tel)")
                    parameter("order", "pac_nombre.asc")
                }

                val arr = JSONArray(res.bodyAsText())
                listaPacientes.clear()

                for (i in 0 until arr.length()) {
                    val o   = arr.getJSONObject(i)
                    val prop = o.optJSONObject("propietarios")
                    listaPacientes.add(
                        PacienteVet(
                            pacId       = o.getInt("pac_id"),
                            nombre      = o.getString("pac_nombre"),
                            especie     = o.getString("pac_especie"),
                            raza        = o.optString("pac_raza", "-"),
                            edad        = if (o.isNull("pac_edad")) null
                            else o.getInt("pac_edad"),
                            propNombre  = prop?.optString("prop_nombre", "Desconocido")
                                ?: "Desconocido",
                            propTel     = prop?.optString("prop_tel", "-") ?: "-"
                        )
                    )
                }

                runOnUiThread { filtrarPacientes(etBuscar.text.toString()) }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@PacientesVetActivity,
                        "Error al cargar pacientes", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun filtrarPacientes(query: String) {
        val filtrados = if (query.isEmpty()) {
            listaPacientes
        } else {
            listaPacientes.filter {
                it.nombre.contains(query, ignoreCase = true) ||
                        it.especie.contains(query, ignoreCase = true) ||
                        it.propNombre.contains(query, ignoreCase = true)
            }
        }

        if (filtrados.isEmpty()) {
            tvVacio.visibility      = View.VISIBLE
            rvPacientes.visibility  = View.GONE
        } else {
            tvVacio.visibility      = View.GONE
            rvPacientes.visibility  = View.VISIBLE
            rvPacientes.adapter = PacienteVetAdapter(filtrados) { paciente ->
                val intent = Intent(this, HistorialVetActivity::class.java)
                intent.putExtra("pacId",      paciente.pacId)
                intent.putExtra("pacNombre",  paciente.nombre)
                intent.putExtra("pacEspecie", paciente.especie)
                startActivity(intent)
            }
        }
    }
}