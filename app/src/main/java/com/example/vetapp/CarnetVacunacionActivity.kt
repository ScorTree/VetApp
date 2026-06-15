package com.example.vetapp

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import android.graphics.Bitmap
import android.graphics.BitmapFactory

class CarnetVacunacionActivity : AppCompatActivity() {

    private var pacId: Int = -1

    // Views
    private lateinit var tvNombreMascota: TextView
    private lateinit var tvInfoMascota: TextView

    private lateinit var tvCantidadVacunas: TextView
    private lateinit var tvProximaVacuna: TextView
    private lateinit var tvEstadoGeneral: TextView

    private lateinit var rvVacunas: RecyclerView
    private lateinit var tvSinVacunas: TextView

    // Adapter
    private lateinit var adapter: VacunaAdapter
    private val listaVacunas = mutableListOf<Vacuna>()

    private var nombreMascota = ""
    private var especieMascota = ""
    private var razaMascota = ""
    private var edadMascota = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carnet_vacunacion)

        pacId = intent.getIntExtra("pacId", -1)

        if (pacId == -1) {
            Toast.makeText(
                this,
                "Mascota inválida",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        tvNombreMascota = findViewById(R.id.tvNombreMascota)
        tvInfoMascota = findViewById(R.id.tvInfoMascota)

        tvCantidadVacunas = findViewById(R.id.tvCantidadVacunas)
        tvProximaVacuna = findViewById(R.id.tvProximaVacuna)
        tvEstadoGeneral = findViewById(R.id.tvEstadoGeneral)

        rvVacunas = findViewById(R.id.rvVacunas)
        tvSinVacunas = findViewById(R.id.tvSinVacunas)

        adapter = VacunaAdapter(listaVacunas)

        rvVacunas.layoutManager = LinearLayoutManager(this)
        rvVacunas.adapter = adapter

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        cargarMascota()
        cargarVacunas()

        findViewById<com.google.android.material.button.MaterialButton>(
            R.id.btnDescargarPdf
        ).setOnClickListener {

            generarPdf()

        }
    }

    // =====================================================
    // CARGAR DATOS DE LA MASCOTA
    // =====================================================

    private fun cargarMascota() {

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val response = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/pacientes"
                ) {

                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(
                            HttpHeaders.Authorization,
                            "Bearer ${SupabaseConfig.API_KEY}"
                        )
                    }

                    parameter("pac_id", "eq.$pacId")
                }

                val array = JSONArray(response.bodyAsText())

                if (array.length() == 0)
                    return@launch

                val obj = array.getJSONObject(0)

                val nombre = obj.getString("pac_nombre")
                val especie = obj.getString("pac_especie")
                val raza = obj.optString("pac_raza", "Sin raza")

                val edad =
                    if (obj.isNull("pac_edad"))
                        "?"
                    else
                        obj.getInt("pac_edad").toString()

                nombreMascota = nombre
                especieMascota = especie
                razaMascota = raza
                edadMascota = edad

                runOnUiThread {

                    tvNombreMascota.text = nombre

                    tvInfoMascota.text =
                        "$especie • $raza • $edad años"

                }

            } catch (e: Exception) {

                runOnUiThread {

                    Toast.makeText(
                        this@CarnetVacunacionActivity,
                        "Error al cargar mascota",
                        Toast.LENGTH_LONG
                    ).show()

                }

            }

        }
    }
    // =====================================================
// CARGAR VACUNAS
// =====================================================

    private fun cargarVacunas() {

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val response = HttpClientProvider.client.get(
                    "${SupabaseConfig.URL}/rest/v1/vacunas"
                ) {

                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(
                            HttpHeaders.Authorization,
                            "Bearer ${SupabaseConfig.API_KEY}"
                        )
                    }

                    parameter("pac_id", "eq.$pacId")
                    parameter("order", "vac_fecha_aplicacion.desc")

                }

                val json = response.bodyAsText()

                val array = JSONArray(json)

                val lista = mutableListOf<Vacuna>()

                for (i in 0 until array.length()) {

                    val obj = array.getJSONObject(i)

                    lista.add(

                        Vacuna(

                            id = obj.getInt("vac_id"),

                            nombre = obj.getString("vac_nombre"),

                            fechaAplicacion =
                                obj.getString("vac_fecha_aplicacion"),

                            fechaProxima =
                                obj.getString("vac_fecha_proxima"),

                            lote =
                                obj.optString("vac_lote", ""),

                            notas =
                                obj.optString("vac_notas", "")

                        )

                    )

                }

                runOnUiThread {

                    listaVacunas.clear()
                    listaVacunas.addAll(lista)

                    adapter.actualizar(listaVacunas)

                    actualizarResumen(lista)

                }

            } catch (e: Exception) {

                runOnUiThread {

                    Toast.makeText(
                        this@CarnetVacunacionActivity,
                        "Error al cargar vacunas",
                        Toast.LENGTH_LONG
                    ).show()

                }

            }

        }

    }

    private fun actualizarResumen(lista: List<Vacuna>) {

        tvCantidadVacunas.text =
            "Vacunas registradas: ${lista.size}"

        if (lista.isEmpty()) {

            rvVacunas.visibility = View.GONE
            tvSinVacunas.visibility = View.VISIBLE

            tvProximaVacuna.text =
                "Próxima vacuna: Sin registros"

            tvEstadoGeneral.text =
                "Estado: Sin información"

            return
        }

        rvVacunas.visibility = View.VISIBLE
        tvSinVacunas.visibility = View.GONE

        val proxima = lista.minByOrNull { it.fechaProxima }

        tvProximaVacuna.text =
            "Próxima vacuna: ${formatearFecha(proxima?.fechaProxima ?: "")}"

        tvEstadoGeneral.text =
            "Estado: Carnet con ${lista.size} vacuna(s)"
    }

    private fun formatearFecha(fecha: String): String {

        return try {

            val origen = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )

            val destino = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

            destino.format(origen.parse(fecha)!!)

        } catch (e: Exception) {

            fecha

        }

    }

    private fun generarPdf() {

        if (listaVacunas.isEmpty()) {
            Toast.makeText(
                this,
                "No hay vacunas para generar el PDF",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        try {

            val pdfDocument = PdfDocument()

            val pageInfo = PdfDocument.PageInfo.Builder(
                612,
                1008,
                1
            ).create()

            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // ==========================
            // LOGO
            // ==========================

            val bitmap = BitmapFactory.decodeResource(
                resources,
                R.drawable.logo_vetapp
            )

            val logo = Bitmap.createScaledBitmap(
                bitmap,
                160,
                160,
                true
            )

            val xLogo = (pageInfo.pageWidth - logo.width) / 2f

            canvas.drawBitmap(
                logo,
                xLogo,
                15f,
                null
            )

            // ==========================
            // PAINTS
            // ==========================

            val titulo = Paint().apply {
                textSize = 24f
                isFakeBoldText = true
            }

            val subtitulo = Paint().apply {
                textSize = 17f
                isFakeBoldText = true
            }

            val texto = Paint().apply {
                textSize = 12f
            }

            val paintNombre = Paint().apply {
                textSize = 19f
                isFakeBoldText = true
                color = android.graphics.Color.rgb(
                    142,
                    124,
                    195
                )
            }

            val encabezadoTabla = Paint().apply {
                textSize = 13f
                isFakeBoldText = true
            }

            val paintTabla = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }

            var y = 205

            // ==========================
            // ENCABEZADO
            // ==========================

            val tituloPrincipal = "VetApp"

            val xTitulo = (pageInfo.pageWidth - titulo.measureText(tituloPrincipal)) / 2

            canvas.drawText(
                tituloPrincipal,
                xTitulo,
                y.toFloat(),
                titulo
            )

            y += 35

            val subtituloTexto = "Carnet de Vacunación"

            val xSubtitulo =
                (pageInfo.pageWidth - subtitulo.measureText(subtituloTexto)) / 2

            canvas.drawText(
                subtituloTexto,
                xSubtitulo,
                y.toFloat(),
                subtitulo
            )

            y += 30

            canvas.drawLine(
                40f,
                y.toFloat(),
                555f,
                y.toFloat(),
                Paint().apply {
                    strokeWidth = 2f
                }
            )

            y += 35

            // ==========================
            // DATOS DE LA MASCOTA
            // ==========================
            val paintRect = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }

            canvas.drawRoundRect(
                35f,
                y.toFloat(),
                560f,
                (y + 130).toFloat(),
                12f,
                12f,
                paintRect
            )

            y += 25

            canvas.drawText(
                "Mascota: ${tvNombreMascota.text}",
                40f,
                y.toFloat(),
                paintNombre
            )

            y += 28

            canvas.drawText(
                tvInfoMascota.text.toString(),
                40f,
                y.toFloat(),
                texto
            )

            y += 24

            val fechaActual = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).format(Date())

            canvas.drawText(
                "Fecha de generación: $fechaActual",
                40f,
                y.toFloat(),
                texto
            )

            y += 22

            canvas.drawText(
                "Vacunas registradas: ${listaVacunas.size}",
                40f,
                y.toFloat(),
                texto
            )

            y += 30

            canvas.drawLine(
                40f,
                y.toFloat(),
                555f,
                y.toFloat(),
                Paint().apply {
                    strokeWidth = 2f
                }
            )

            y += 35

            // ==========================
// VACUNAS
// ==========================

            val xInicio = 40f
            val xVacuna = 180f
            val xAplicacion = 300f
            val xProxima = 420f
            val xLote = 555f

            val altoFila = 30f

// ==========================
// ENCABEZADO DE LA TABLA
// ==========================

            canvas.drawRect(
                xInicio,
                y.toFloat(),
                xLote,
                (y + altoFila).toFloat(),
                paintTabla
            )

// Divisiones verticales
            canvas.drawLine(xVacuna, y.toFloat(), xVacuna, (y + altoFila).toFloat(), paintTabla)
            canvas.drawLine(xAplicacion, y.toFloat(), xAplicacion, (y + altoFila).toFloat(), paintTabla)
            canvas.drawLine(xProxima, y.toFloat(), xProxima, (y + altoFila).toFloat(), paintTabla)

// Encabezados
            canvas.drawText(
                "Vacuna",
                50f,
                (y + 20).toFloat(),
                encabezadoTabla
            )

            canvas.drawText(
                "Aplicación",
                190f,
                (y + 20).toFloat(),
                encabezadoTabla
            )

            canvas.drawText(
                "Próxima",
                310f,
                (y + 20).toFloat(),
                encabezadoTabla
            )

            canvas.drawText(
                "Lote",
                435f,
                (y + 20).toFloat(),
                encabezadoTabla
            )

            y += altoFila.toInt()

// ==========================
// FILAS DE LA TABLA
// ==========================

            for (vacuna in listaVacunas) {

                canvas.drawRect(
                    xInicio,
                    y.toFloat(),
                    xLote,
                    (y + altoFila).toFloat(),
                    paintTabla
                )

                canvas.drawLine(
                    xVacuna,
                    y.toFloat(),
                    xVacuna,
                    (y + altoFila).toFloat(),
                    paintTabla
                )

                canvas.drawLine(
                    xAplicacion,
                    y.toFloat(),
                    xAplicacion,
                    (y + altoFila).toFloat(),
                    paintTabla
                )

                canvas.drawLine(
                    xProxima,
                    y.toFloat(),
                    xProxima,
                    (y + altoFila).toFloat(),
                    paintTabla
                )

                canvas.drawText(
                    vacuna.nombre,
                    50f,
                    (y + 20).toFloat(),
                    texto
                )

                canvas.drawText(
                    formatearFecha(vacuna.fechaAplicacion),
                    190f,
                    (y + 20).toFloat(),
                    texto
                )

                canvas.drawText(
                    formatearFecha(vacuna.fechaProxima),
                    310f,
                    (y + 20).toFloat(),
                    texto
                )

                canvas.drawText(
                    vacuna.lote ?: "-",
                    435f,
                    (y + 20).toFloat(),
                    texto
                )

                y += altoFila.toInt()
            }

// ==========================
// NOTAS
// ==========================

            y += 35

            canvas.drawText(
                "Notas",
                40f,
                y.toFloat(),
                subtitulo
            )

            y += 25

            for (vacuna in listaVacunas) {

                if (!vacuna.notas.isNullOrBlank()) {

                    canvas.drawText(
                        "• ${vacuna.nombre}:",
                        50f,
                        y.toFloat(),
                        subtitulo
                    )

                    y += 18

                    canvas.drawText(
                        vacuna.notas,
                        65f,
                        y.toFloat(),
                        texto
                    )

                    y += 25
                }
            }

            // ==========================
// PIE DEL DOCUMENTO
// ==========================

            val pieY = pageInfo.pageHeight - 50

// Línea separadora
            canvas.drawLine(
                40f,
                (pieY - 15).toFloat(),
                570f,
                (pieY - 15).toFloat(),
                paintTabla
            )

// Estado general
            canvas.drawText(
                "Estado general: Carnet con ${listaVacunas.size} vacuna(s) registradas",
                40f,
                (pieY - 25).toFloat(),
                texto
            )

// Pie del documento
            canvas.drawText(
                "Documento generado automáticamente por VetApp",
                40f,
                pieY.toFloat(),
                texto
            )

            canvas.drawText(
                "Fecha de emisión: $fechaActual",
                40f,
                (pieY + 18).toFloat(),
                texto
            )

            pdfDocument.finishPage(page)

            val carpeta = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )

            if (!carpeta.exists()) {
                carpeta.mkdirs()
            }

            val fechaArchivo = SimpleDateFormat(
                "yyyyMMdd",
                Locale.getDefault()
            ).format(Date())

            val archivo = File(
                carpeta,
                "Carnet_Vacunacion_${tvNombreMascota.text}_$fechaArchivo.pdf"
            )

            pdfDocument.writeTo(
                FileOutputStream(archivo)
            )

            pdfDocument.close()

            Toast.makeText(
                this,
                "PDF guardado en:\n${archivo.absolutePath}",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Error al generar PDF:\n${e.message}",
                Toast.LENGTH_LONG
            ).show()

            e.printStackTrace()
        }
    }
}