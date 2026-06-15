package com.example.vetapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import com.example.vetapp.util.SessionManager
import com.google.android.material.card.MaterialCardView
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private lateinit var rvMascotas: RecyclerView
    private lateinit var mascotaAdapter: MascotaAdapter
    private lateinit var layoutContenido: View
    private val listaMascotas = mutableListOf<Mascota>()
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var btnCalificacion: ImageButton

    private val handler = android.os.Handler(
        android.os.Looper.getMainLooper()
    )

    private val animacionRunnable = object : Runnable {

        override fun run() {

            val animacion =
                android.view.animation.AnimationUtils.loadAnimation(
                    this@MainActivity,
                    R.anim.bounce
                )

            btnCalificacion.startAnimation(animacion)

            // Repetir cada 15 segundos
            handler.postDelayed(this, 15000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnLogout = findViewById<ImageButton>(R.id.btnLogout)
        btnCalificacion = findViewById(R.id.btnCalificacion)
        val cardAddPet = findViewById<MaterialCardView>(R.id.cardAddPet)
        rvMascotas = findViewById(R.id.rvMascotas)
        layoutEmpty = findViewById(R.id.layoutEmpty)

        // RecyclerView
        mascotaAdapter = MascotaAdapter(listaMascotas) { mascota ->
            val intent = Intent(this, DetalleMascotaActivity::class.java)
            intent.putExtra("pacId", mascota.pacId)
            intent.putExtra("nombre", mascota.nombre)
            intent.putExtra("especie", mascota.especie)
            intent.putExtra("raza", mascota.raza)
            intent.putExtra("edad", mascota.edad ?: -1)
            startActivity(intent)
        }
        rvMascotas.layoutManager = LinearLayoutManager(this)
        rvMascotas.adapter = mascotaAdapter
        rvMascotas.isNestedScrollingEnabled = true

        rvMascotas.layoutAnimation =
            android.view.animation.AnimationUtils.loadLayoutAnimation(
                this,
                R.anim.layout_fall_down
            )

        layoutContenido = findViewById(R.id.layoutContenido)

        // Logout
        btnLogout.setOnClickListener {
            SessionManager(this).logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Agregar mascota
        cardAddPet.setOnClickListener {
            startActivity(Intent(this, AgregarMascotaActivity::class.java))
        }

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            R.id.bottomNavigation
        )

// Marcar INICIO como seleccionado
        bottomNav.selectedItemId = R.id.menu_inicio

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.menu_inicio -> {
                    true
                }

                R.id.menu_cita -> {
                    startActivity(
                        Intent(this, HacerCitaActivity::class.java)
                    )
                    overridePendingTransition(0, 0)
                    finish()
                    false
                }

                R.id.menu_mis_citas -> {
                    startActivity(
                        Intent(this, MisCitasActivity::class.java)
                    )
                    overridePendingTransition(0, 0)
                    finish()
                    false
                }

                else -> false
            }
        }

        btnCalificacion.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    CalificacionActivity::class.java
                )
            )

        }

    }

    override fun onResume() {
        super.onResume()
        cargarMascotas() // se ejecuta cada vez que vuelves
        handler.post(animacionRunnable)
    }
    override fun onPause() {

        super.onPause()

        handler.removeCallbacks(animacionRunnable)

    }

    private fun cargarMascotas() {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val email = SessionManager(this@MainActivity).getEmail()
                if (email == null) return@launch

                // Obtener prop_id
                val propResponse = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/propietarios"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("prop_email", "eq.$email")
                    parameter("select", "prop_id")
                }

                val propArray = JSONArray(propResponse.bodyAsText())
                if (propArray.length() == 0) return@launch

                val propId = propArray.getJSONObject(0).getInt("prop_id")

                // Obtener mascotas
                val mascotasResponse = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/pacientes"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("prop_id", "eq.$propId")
                    parameter("select", "pac_id,pac_nombre,pac_especie,pac_raza,pac_edad")
                }

                val mascotasArray = JSONArray(mascotasResponse.bodyAsText())

                listaMascotas.clear()

                for (i in 0 until mascotasArray.length()) {
                    val obj = mascotasArray.getJSONObject(i)
                    listaMascotas.add(
                        Mascota(
                            pacId = obj.getInt("pac_id"),
                            nombre = obj.getString("pac_nombre"),
                            especie = obj.getString("pac_especie"),
                            raza = obj.optString("pac_raza", null),
                            edad = if (obj.isNull("pac_edad")) null else obj.getInt("pac_edad")
                        )
                    )
                }

                runOnUiThread {
                    if (listaMascotas.isEmpty()) {
                        layoutEmpty.visibility = View.VISIBLE
                        layoutContenido.visibility = View.GONE
                    } else {
                        layoutEmpty.visibility = View.GONE
                        layoutContenido.visibility = View.VISIBLE
                    }
                    mascotaAdapter.notifyDataSetChanged()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Error al cargar mascotas",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
