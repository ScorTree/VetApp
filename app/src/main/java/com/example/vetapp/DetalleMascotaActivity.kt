package com.example.vetapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Locale

class DetalleMascotaActivity : AppCompatActivity() {

    private var pacId: Int = -1

    // ---------- Datos mascota ----------
    private lateinit var tvNombre: TextView
    private lateinit var tvEspecie: TextView
    private lateinit var tvRaza: TextView
    private lateinit var tvEdad: TextView

    // ---------- RecyclerViews ----------
    private lateinit var rvHistorial: RecyclerView
    private lateinit var rvMedicinas: RecyclerView

    // ---------- Textos vacío ----------
    private lateinit var tvHistorialVacio: TextView
    private lateinit var tvMedicinasVacio: TextView

    // ---------- Adapters ----------
    private lateinit var historialAdapter: HistorialGeneralAdapter
    private lateinit var medicinaAdapter: MedicinaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_mascota)

        // ---------- pacId ----------
        pacId = intent.getIntExtra("pacId", -1)
        if (pacId == -1) {
            Toast.makeText(this, "Mascota inválida", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // ---------- Views ----------
        tvNombre = findViewById(R.id.tvNombreMascota)
        tvEspecie = findViewById(R.id.tvEspecie)
        tvRaza = findViewById(R.id.tvRaza)
        tvEdad = findViewById(R.id.tvEdad)

        rvHistorial = findViewById(R.id.rvHistorial)
        rvMedicinas = findViewById(R.id.rvMedicinas)

        tvHistorialVacio = findViewById(R.id.tvHistorialVacio)
        tvMedicinasVacio = findViewById(R.id.tvMedicinasVacio)

        // ---------- Recycler ----------
        rvHistorial.layoutManager = LinearLayoutManager(this)
        rvMedicinas.layoutManager = LinearLayoutManager(this)

        historialAdapter = HistorialGeneralAdapter(mutableListOf())
        medicinaAdapter = MedicinaAdapter(mutableListOf())

        rvHistorial.adapter = historialAdapter
        rvMedicinas.adapter = medicinaAdapter

        // ---------- Botones ----------
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnEditarMascota).setOnClickListener {
            startActivity(
                Intent(this, EditarMascotaActivity::class.java)
                    .putExtra("pacId", pacId)
            )
        }

        findViewById<MaterialButton>(R.id.btnEliminarMascota).setOnClickListener {
            confirmarEliminar()
        }

        findViewById<MaterialButton>(R.id.btnCarnetVacunacion).setOnClickListener {

            val intent = Intent(
                this,
                CarnetVacunacionActivity::class.java
            )

            intent.putExtra("pacId", pacId)

            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.btnCarnetVacunacion).setOnClickListener {
            startActivity(
                Intent(this, CarnetVacunacionActivity::class.java)
                    .putExtra("pacId", pacId)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        cargarMascota()
        cargarHistorial()
        cargarMedicinas()
    }

    // =====================================================
    // MASCOTA
    // =====================================================
    private fun cargarMascota() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/pacientes"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("pac_id", "eq.$pacId")
                }

                val obj = JSONArray(response.bodyAsText()).getJSONObject(0)

                runOnUiThread {
                    tvNombre.text = obj.getString("pac_nombre")
                    tvEspecie.text = getString(
                        R.string.detalle_especie,
                        obj.getString("pac_especie")
                    )
                    tvRaza.text = getString(
                        R.string.detalle_raza,
                        obj.optString("pac_raza", getString(R.string.sin_raza))
                    )
                    tvEdad.text = getString(
                        R.string.detalle_edad,
                        if (obj.isNull("pac_edad")) "?"
                        else obj.getInt("pac_edad").toString()
                    )
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@DetalleMascotaActivity,
                        "Error al cargar mascota",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // =====================================================
    // HISTORIAL GENERAL
    // =====================================================
    private fun cargarHistorial() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/historial_general"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("pac_id", "eq.$pacId")
                    parameter("order", "hist_fecha.desc")
                }

                val array = JSONArray(response.bodyAsText())
                val lista = mutableListOf<HistorialGeneral>()

                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)

                    lista.add(
                        HistorialGeneral(
                            histId = o.getInt("hist_id"),
                            fecha = formatearFecha(o.getString("hist_fecha")),
                            peso = if (o.isNull("hist_peso")) null else o.getDouble("hist_peso"),
                            temperatura = if (o.isNull("hist_temp")) null else o.getDouble("hist_temp"),
                            diagnostico = o.optString("hist_diag", null),
                            notas = o.optString("hist_notas", null)
                        )
                    )
                }

                runOnUiThread {
                    if (lista.isEmpty()) {
                        tvHistorialVacio.visibility = View.VISIBLE
                        rvHistorial.visibility = View.GONE
                    } else {
                        tvHistorialVacio.visibility = View.GONE
                        rvHistorial.visibility = View.VISIBLE
                        historialAdapter.setData(lista)
                    }
                }

            } catch (_: Exception) { }
        }
    }

    // =====================================================
    // MEDICINAS
    // =====================================================
    private fun cargarMedicinas() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/historial_medicinas"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("pac_id", "eq.$pacId")
                }

                val array = JSONArray(response.bodyAsText())
                val lista = mutableListOf<Medicina>()

                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)

                    lista.add(
                        Medicina(
                            nombre = o.getString("med_nombre"),
                            dosis = o.getString("med_dosis"),
                            fecha = o.getString("med_fecha"),
                            costo = o.getDouble("med_costo")
                        )
                    )
                }

                runOnUiThread {
                    if (lista.isEmpty()) {
                        tvMedicinasVacio.visibility = View.VISIBLE
                        rvMedicinas.visibility = View.GONE
                    } else {
                        tvMedicinasVacio.visibility = View.GONE
                        rvMedicinas.visibility = View.VISIBLE
                        medicinaAdapter.actualizar(lista)
                    }
                }

            } catch (_: Exception) { }
        }
    }

    // =====================================================
    // ELIMINAR
    // =====================================================
    private fun confirmarEliminar() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar mascota")
            .setMessage("¿Seguro que deseas eliminar esta mascota?")
            .setPositiveButton("Eliminar") { _, _ -> eliminarMascota() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarMascota() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                HttpClientProvider.client.delete(
                    "${SupabaseConfig.URL}/rest/v1/pacientes"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("pac_id", "eq.$pacId")
                }

                runOnUiThread {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Mascota eliminada",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    finish()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@DetalleMascotaActivity,
                        "Error al eliminar mascota",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // =====================================================
    // FORMATO FECHA (API 24)
    // =====================================================
    private fun formatearFecha(fecha: String): String {
        return try {
            val original = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val destino = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            destino.format(original.parse(fecha)!!)
        } catch (e: Exception) {
            fecha
        }
    }
}
