package com.example.vetapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class HistorialVetActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_vet)

        val pacId     = intent.getIntExtra("pacId", -1)
        val pacNombre = intent.getStringExtra("pacNombre") ?: ""
        val pacEspecie = intent.getStringExtra("pacEspecie") ?: ""

        findViewById<TextView>(R.id.tvHistVetNombre).text  = pacNombre
        findViewById<TextView>(R.id.tvHistVetEspecie).text = pacEspecie

        findViewById<android.widget.ImageButton>(R.id.btnBackHistVet)
            .setOnClickListener { finish() }

        val rvHistorial = findViewById<RecyclerView>(R.id.rvHistorialVet)
        val tvVacio     = findViewById<TextView>(R.id.tvHistorialVetVacio)
        rvHistorial.layoutManager = LinearLayoutManager(this)

        if (pacId != -1) cargarHistorial(pacId, rvHistorial, tvVacio)
    }

    private fun cargarHistorial(
        pacId: Int,
        rvHistorial: RecyclerView,
        tvVacio: TextView
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/historial_general"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }
                    parameter("pac_id", "eq.$pacId")
                    parameter("select",
                        "hist_id,hist_fecha,hist_peso,hist_temp,hist_diag,hist_notas")
                    parameter("order", "hist_fecha.desc")
                }

                val arr = JSONArray(res.bodyAsText())
                val lista = mutableListOf<HistorialGeneral>()

                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    lista.add(
                        HistorialGeneral(
                            histId      = o.getInt("hist_id"),
                            fecha       = o.getString("hist_fecha"),
                            peso        = if (o.isNull("hist_peso")) null else o.getDouble("hist_peso"),
                            temp        = if (o.isNull("hist_temp")) null else o.getDouble("hist_temp"),
                            diagnostico = o.getString("hist_diag"),
                            notas       = o.optString("hist_notas", "")
                        )
                    )
                }

                runOnUiThread {
                    if (lista.isEmpty()) {
                        tvVacio.visibility    = View.VISIBLE
                        rvHistorial.visibility = View.GONE
                    } else {
                        tvVacio.visibility    = View.GONE
                        rvHistorial.visibility = View.VISIBLE
                        rvHistorial.adapter = HistorialVetAdapter(lista)
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@HistorialVetActivity,
                        "Error al cargar historial", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}