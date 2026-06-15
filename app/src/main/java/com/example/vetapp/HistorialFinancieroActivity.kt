package com.example.vetapp

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Locale

class HistorialFinancieroActivity : AppCompatActivity() {

    private lateinit var rvPagos: RecyclerView
    private lateinit var tvSinPagos: TextView
    private lateinit var tvResumenFinanciero: TextView

    private lateinit var adapter: PagoAdapter
    private val listaPagos = mutableListOf<Pago>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_financiero)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        rvPagos = findViewById(R.id.rvPagos)
        tvSinPagos = findViewById(R.id.tvSinPagos)
        tvResumenFinanciero = findViewById(R.id.tvResumenFinanciero)

        adapter = PagoAdapter(listaPagos)

        rvPagos.layoutManager = LinearLayoutManager(this)
        rvPagos.adapter = adapter

        cargarPagos()
    }

    private fun cargarPagos() {
        lifecycleScope.launch {
            try {
                val response = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/pagos"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(HttpHeaders.Authorization, "Bearer ${SupabaseConfig.API_KEY}")
                    }

                    parameter(
                        "select",
                        "pago_id,pago_monto,pago_fecha,pago_estado,pago_conc,cita_id,pago_metodo,pago_referencia"
                    )
                    parameter("order", "pago_fecha.desc")
                }

                val array = JSONArray(response.bodyAsText())
                val nuevaLista = mutableListOf<Pago>()

                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)

                    nuevaLista.add(
                        Pago(
                            id = o.getInt("pago_id"),
                            monto = o.optDouble("pago_monto", 0.0),
                            fecha = o.optString("pago_fecha", ""),
                            estado = o.optString("pago_estado", "Pagado"),
                            concepto = o.optString("pago_conc", "Pago de servicio"),
                            citaId = if (o.isNull("cita_id")) null else o.getInt("cita_id"),
                            metodo = o.optString("pago_metodo", "Tarjeta"),
                            referencia = o.optString("pago_referencia", "Sin referencia")
                        )
                    )
                }

                val total = nuevaLista.sumOf { it.monto }

                listaPagos.clear()
                listaPagos.addAll(nuevaLista)

                adapter.notifyDataSetChanged()

                tvResumenFinanciero.text =
                    "Total pagado: $${String.format(Locale.getDefault(), "%.2f", total)}"

                if (listaPagos.isEmpty()) {
                    tvSinPagos.visibility = View.VISIBLE
                    rvPagos.visibility = View.GONE
                } else {
                    tvSinPagos.visibility = View.GONE
                    rvPagos.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@HistorialFinancieroActivity,
                    "Error al cargar historial financiero",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}