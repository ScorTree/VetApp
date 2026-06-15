package com.example.vetapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import com.example.vetapp.util.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import android.content.Intent

class InventarioActivity : AppCompatActivity() {

    private val listaMeds = mutableListOf<MedicamentoInventario>()
    private lateinit var rvInventario: RecyclerView
    private lateinit var tvVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventario)

        rvInventario = findViewById(R.id.rvInventario)
        tvVacio      = findViewById(R.id.tvInventarioVacio)

        rvInventario.layoutManager = LinearLayoutManager(this)

        findViewById<MaterialButton>(R.id.btnAgregarMed).setOnClickListener {
            mostrarDialogoAgregar()
        }

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
                R.id.vet_menu_inventario -> true
                R.id.vet_menu_perfil -> {
                    startActivity(Intent(this, PerfilVetActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }

        cargarInventario()
    }

    override fun onResume() {
        super.onResume()
        cargarInventario()
    }

    private fun cargarInventario() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/medicamentos"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("med_activo", "eq.true")
                    parameter("select",
                        "med_id,med_nombre,med_lote,med_caducidad,med_precio,med_stock")
                    parameter("order", "med_nombre.asc")
                }

                val arr = JSONArray(res.bodyAsText())
                listaMeds.clear()

                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    listaMeds.add(
                        MedicamentoInventario(
                            medId      = o.getInt("med_id"),
                            nombre     = o.getString("med_nombre"),
                            lote       = o.optString("med_lote", "-"),
                            caducidad  = o.optString("med_caducidad", "-"),
                            precio     = o.optDouble("med_precio", 0.0),
                            stock      = o.getInt("med_stock")
                        )
                    )
                }

                runOnUiThread {
                    if (listaMeds.isEmpty()) {
                        tvVacio.visibility      = View.VISIBLE
                        rvInventario.visibility = View.GONE
                    } else {
                        tvVacio.visibility      = View.GONE
                        rvInventario.visibility = View.VISIBLE
                        rvInventario.adapter = InventarioAdapter(listaMeds,
                            onEditar  = { med -> mostrarDialogoEditar(med) },
                            onEliminar = { med -> confirmarEliminar(med) }
                        )
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@InventarioActivity,
                        "Error al cargar inventario", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarDialogoAgregar() {
        val view = layoutInflater.inflate(R.layout.dialog_medicamento, null)
        AlertDialog.Builder(this)
            .setTitle("Agregar medicamento")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre    = view.findViewById<TextInputEditText>(R.id.etMedNombre).text.toString().trim()
                val lote      = view.findViewById<TextInputEditText>(R.id.etMedLote).text.toString().trim()
                val caducidad = view.findViewById<TextInputEditText>(R.id.etMedCaducidad).text.toString().trim()
                val precio    = view.findViewById<TextInputEditText>(R.id.etMedPrecio).text.toString().trim()
                val stock     = view.findViewById<TextInputEditText>(R.id.etMedStock).text.toString().trim()

                if (nombre.isEmpty() || stock.isEmpty()) {
                    Toast.makeText(this, "Nombre y stock son obligatorios",
                        Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                agregarMedicamento(nombre, lote, caducidad, precio, stock.toInt())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEditar(med: MedicamentoInventario) {
        val view = layoutInflater.inflate(R.layout.dialog_medicamento, null)
        view.findViewById<TextInputEditText>(R.id.etMedNombre).setText(med.nombre)
        view.findViewById<TextInputEditText>(R.id.etMedLote).setText(med.lote)
        view.findViewById<TextInputEditText>(R.id.etMedCaducidad).setText(med.caducidad)
        view.findViewById<TextInputEditText>(R.id.etMedPrecio).setText(med.precio.toString())
        view.findViewById<TextInputEditText>(R.id.etMedStock).setText(med.stock.toString())

        AlertDialog.Builder(this)
            .setTitle("Editar medicamento")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre    = view.findViewById<TextInputEditText>(R.id.etMedNombre).text.toString().trim()
                val lote      = view.findViewById<TextInputEditText>(R.id.etMedLote).text.toString().trim()
                val caducidad = view.findViewById<TextInputEditText>(R.id.etMedCaducidad).text.toString().trim()
                val precio    = view.findViewById<TextInputEditText>(R.id.etMedPrecio).text.toString().trim()
                val stock     = view.findViewById<TextInputEditText>(R.id.etMedStock).text.toString().trim()

                if (nombre.isEmpty() || stock.isEmpty()) {
                    Toast.makeText(this, "Nombre y stock son obligatorios",
                        Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                editarMedicamento(med.medId, nombre, lote, caducidad, precio, stock.toInt())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminar(med: MedicamentoInventario) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar medicamento")
            .setMessage("¿Deseas eliminar ${med.nombre}?")
            .setPositiveButton("Eliminar") { _, _ -> eliminarMedicamento(med.medId) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun agregarMedicamento(
        nombre: String, lote: String, caducidad: String,
        precio: String, stock: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = buildString {
                    append("{")
                    append("\"med_nombre\":\"$nombre\",")
                    append("\"med_stock\":$stock,")
                    append("\"med_activo\":true")
                    if (lote.isNotEmpty())      append(",\"med_lote\":\"$lote\"")
                    if (caducidad.isNotEmpty()) append(",\"med_caducidad\":\"$caducidad\"")
                    if (precio.isNotEmpty())    append(",\"med_precio\":$precio")
                    append("}")
                }

                HttpClientProvider.client.post(
                    "${SupabaseConfig.URL}/rest/v1/medicamentos"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                        append(HttpHeaders.ContentType, "application/json")
                        append("Prefer", "return=minimal")
                    }
                    setBody(body)
                }

                runOnUiThread {
                    Toast.makeText(this@InventarioActivity,
                        "Medicamento agregado ✓", Toast.LENGTH_SHORT).show()
                    cargarInventario()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@InventarioActivity,
                        "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun editarMedicamento(
        medId: Int, nombre: String, lote: String, caducidad: String,
        precio: String, stock: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = buildString {
                    append("{")
                    append("\"med_nombre\":\"$nombre\",")
                    append("\"med_stock\":$stock,")
                    append("\"med_lote\":\"$lote\"")
                    if (caducidad.isNotEmpty()) append(",\"med_caducidad\":\"$caducidad\"")
                    if (precio.isNotEmpty())    append(",\"med_precio\":$precio")
                    append("}")
                }

                HttpClientProvider.client.patch(
                    "${SupabaseConfig.URL}/rest/v1/medicamentos"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                        append(HttpHeaders.ContentType, "application/json")
                        append("Prefer", "return=minimal")
                    }
                    parameter("med_id", "eq.$medId")
                    setBody(body)
                }

                runOnUiThread {
                    Toast.makeText(this@InventarioActivity,
                        "Medicamento actualizado ✓", Toast.LENGTH_SHORT).show()
                    cargarInventario()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@InventarioActivity,
                        "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun eliminarMedicamento(medId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Baja lógica, no borrado físico
                HttpClientProvider.client.patch(
                    "${SupabaseConfig.URL}/rest/v1/medicamentos"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                        append(HttpHeaders.ContentType, "application/json")
                        append("Prefer", "return=minimal")
                    }
                    parameter("med_id", "eq.$medId")
                    setBody("""{"med_activo":false}""")
                }

                runOnUiThread {
                    Toast.makeText(this@InventarioActivity,
                        "Medicamento eliminado ✓", Toast.LENGTH_SHORT).show()
                    cargarInventario()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@InventarioActivity,
                        "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}