package com.example.vetapp

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Calendar
import java.util.Locale

class HacerCitaActivity : AppCompatActivity() {

    private lateinit var spMascota: TextInputEditText
    private lateinit var spMotivo: TextInputEditText
    private lateinit var etFecha: TextInputEditText
    private lateinit var etHora: TextInputEditText
    private lateinit var btnConfirmar: MaterialButton

    private lateinit var rvHorarios: RecyclerView
    private lateinit var layoutDisponibilidad: LinearLayout
    private lateinit var tvDisponibilidadTitulo: TextView

    private var diaSeleccionado = -1
    private val listaMascotas = mutableListOf<Mascota>()
    private var mascotaSeleccionada: Mascota? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hacer_cita)

        spMascota = findViewById(R.id.spMascota)
        spMotivo = findViewById(R.id.spMotivo)
        etFecha = findViewById(R.id.etFecha)
        etHora = findViewById(R.id.etHora)
        btnConfirmar = findViewById(R.id.btnConfirmar)

        rvHorarios = findViewById(R.id.rvHorarios)
        layoutDisponibilidad = findViewById(R.id.layoutDisponibilidad)
        tvDisponibilidadTitulo = findViewById(R.id.tvDisponibilidadTitulo)

        rvHorarios.layoutManager = LinearLayoutManager(this)

        // Inputs solo clic, no escritura
        spMascota.isFocusable = false
        spMotivo.isFocusable = false
        etFecha.isFocusable = false

        etHora.isFocusable = false
        etHora.isClickable = false

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.menu_cita

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.menu_inicio -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.menu_mis_citas -> {
                    startActivity(Intent(this, MisCitasActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> true
            }
        }

        spMascota.setOnClickListener { cargarMascotas() }
        spMotivo.setOnClickListener { mostrarMotivos() }
        etFecha.setOnClickListener { mostrarDatePicker() }
        btnConfirmar.setOnClickListener { guardarCita() }
    }

    // ===================== FECHA =====================
    private fun mostrarDatePicker() {

        val hoy = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            this,
            { _, y, m, d ->

                val seleccion = Calendar.getInstance()
                seleccion.set(y, m, d)
                diaSeleccionado = seleccion.get(Calendar.DAY_OF_WEEK)

                // Domingo no disponible
                if (diaSeleccionado == Calendar.SUNDAY) {
                    Toast.makeText(
                        this,
                        "Domingos no disponibles",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@DatePickerDialog
                }

                // Mostrar fecha seleccionada
                etFecha.setText(
                    String.format(
                        Locale.getDefault(),
                        "%02d/%02d/%d",
                        d,
                        m + 1,
                        y
                    )
                )

                // Limpiar hora previa
                etHora.setText("")

                // Fecha en formato ISO
                val fechaISO = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    y,
                    m + 1,
                    d
                )

                // Verificar si la fecha es HOY
                val esHoy =
                    y == hoy.get(Calendar.YEAR) &&
                            m == hoy.get(Calendar.MONTH) &&
                            d == hoy.get(Calendar.DAY_OF_MONTH)

                lifecycleScope.launch {

                    val listaHorarios =
                        obtenerHorariosDisponibles(fechaISO, esHoy)

                    // Si es hoy y ya no hay horarios → bloquear completamente
                    if (esHoy && listaHorarios.none { it.disponible }) {
                        Toast.makeText(
                            this@HacerCitaActivity,
                            "Ya no hay horarios disponibles hoy",
                            Toast.LENGTH_LONG
                        ).show()

                        rvHorarios.visibility = View.GONE
                        etHora.setText("")
                        return@launch
                    }

                    // Mostrar horarios disponibles
                    rvHorarios.adapter =
                        HorarioAdapter(listaHorarios) { horario ->
                            if (horario.disponible) {
                                etHora.setText(horario.hora)
                            }
                        }

                    rvHorarios.visibility = View.VISIBLE
                }
            },
            hoy.get(Calendar.YEAR),
            hoy.get(Calendar.MONTH),
            hoy.get(Calendar.DAY_OF_MONTH)
        )

        // Bloquear días pasados
        datePickerDialog.datePicker.minDate = hoy.timeInMillis

        datePickerDialog.show()
    }


    // ===================== HORARIOS =====================
    private fun generarHorarios(): List<String> {
        val lista = mutableListOf<String>()

        val inicio = 13
        val fin = if (diaSeleccionado == Calendar.SATURDAY) 17 else 20

        var h = inicio
        var m = 0

        while (h < fin) {
            lista.add(String.format(Locale.getDefault(), "%02d:%02d", h, m))
            m += 30
            if (m == 60) {
                m = 0
                h++
            }
        }
        return lista
    }

    private suspend fun obtenerHorariosDisponibles(
        fechaISO: String,
        esHoy: Boolean
    ): List<HorarioDisponible> {

        // Obtener citas ocupadas
        val res = HttpClientProvider.client.get("${SupabaseConfig.URL}/rest/v1/citas") {
            headers {
                append("apikey", SupabaseConfig.API_KEY)
                append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
            }
            parameter("cita_fecha", "eq.$fechaISO")
            parameter("select", "cita_hora")
        }

        val arr = JSONArray(res.bodyAsText())
        val horasOcupadas = mutableSetOf<String>()

        for (i in 0 until arr.length()) {
            horasOcupadas.add(
                arr.getJSONObject(i).getString("cita_hora").substring(0, 5)
            )
        }

        // Hora actual SOLO si es hoy
        val ahora = Calendar.getInstance()
        val minutosActuales =
            ahora.get(Calendar.HOUR_OF_DAY) * 60 +
                    ahora.get(Calendar.MINUTE)

        return generarHorarios()
            .filter { hora ->
                if (!esHoy) return@filter true

                val partes = hora.split(":")
                val minutosHora =
                    partes[0].toInt() * 60 + partes[1].toInt()

                minutosHora > minutosActuales
            }
            .map { hora ->
                HorarioDisponible(
                    hora = hora,
                    disponible = !horasOcupadas.contains(hora)
                )
            }
    }

    // ===================== GUARDAR =====================
    private fun guardarCita() {
        if (
            etFecha.text.isNullOrEmpty() ||
            etHora.text.isNullOrEmpty() ||
            spMotivo.text.isNullOrEmpty() ||
            mascotaSeleccionada == null
        ) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val f = etFecha.text.toString().split("/")
        val fechaISO = "${f[2]}-${f[1]}-${f[0]}"
        val horaISO = "${etHora.text}:00"

        lifecycleScope.launch {
            try {
                val body = """
                {
                  "cita_fecha":"$fechaISO",
                  "cita_hora":"$horaISO",
                  "cita_motivo":"${spMotivo.text}",
                  "pac_id":${mascotaSeleccionada!!.pacId}
                }
                """.trimIndent()

                HttpClientProvider.client.post("${SupabaseConfig.URL}/rest/v1/citas") {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                        append(HttpHeaders.ContentType, "application/json")
                        append("Prefer", "return=minimal")
                    }
                    setBody(body)
                }

                Toast.makeText(this@HacerCitaActivity, "Cita registrada", Toast.LENGTH_SHORT).show()
                limpiar()

            } catch (e: Exception) {
                Log.e("SUPABASE", "INSERT", e)
                Toast.makeText(this@HacerCitaActivity, "Error al registrar cita", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun limpiar() {
        spMascota.setText("")
        spMotivo.setText("")
        etFecha.setText("")
        etHora.setText("")
        mascotaSeleccionada = null
        rvHorarios.visibility = View.GONE
    }

    // ===================== MASCOTAS =====================
    private fun cargarMascotas() {
        lifecycleScope.launch {
            try {
                val email = SessionManager(this@HacerCitaActivity).getEmail() ?: return@launch

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

                val mascRes = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/pacientes"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("prop_id", "eq.$propId")
                    parameter("select", "pac_id,pac_nombre,pac_especie,pac_raza,pac_edad")
                }

                val arr = JSONArray(mascRes.bodyAsText())
                listaMascotas.clear()

                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    listaMascotas.add(
                        Mascota(
                            o.getInt("pac_id"),
                            o.getString("pac_nombre"),
                            o.getString("pac_especie"),
                            o.optString("pac_raza", null),
                            if (o.isNull("pac_edad")) null else o.getInt("pac_edad")
                        )
                    )
                }

                mostrarDialogoMascotas()

            } catch (e: Exception) {
                Log.e("SUPABASE", "Mascotas", e)
                Toast.makeText(this@HacerCitaActivity, "Error al cargar mascotas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoMascotas() {
        if (listaMascotas.isEmpty()) {
            Toast.makeText(this, "No tienes mascotas registradas", Toast.LENGTH_SHORT).show()
            return
        }

        val nombres = listaMascotas.map { it.nombre }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Selecciona una mascota")
            .setItems(nombres) { _, i ->
                mascotaSeleccionada = listaMascotas[i]
                spMascota.setText(mascotaSeleccionada!!.nombre)
            }
            .show()
    }

    private fun mostrarMotivos() {
        val motivos = arrayOf("Chequeo general", "Vacunación", "Desparasitación")
        AlertDialog.Builder(this)
            .setTitle("Motivo")
            .setItems(motivos) { _, i -> spMotivo.setText(motivos[i]) }
            .show()
    }
}
