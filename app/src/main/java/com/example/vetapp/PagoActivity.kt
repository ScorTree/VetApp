package com.example.vetapp

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import android.text.Editable
import android.text.TextWatcher
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.io.FileOutputStream
import com.example.vetapp.data.HttpClientProvider
import com.example.vetapp.data.SupabaseConfig
import com.google.android.material.button.MaterialButton
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PagoActivity : AppCompatActivity() {

    private var citaId = -1
    private var costo = 0.0
    private lateinit var mascota: String
    private lateinit var motivo: String
    private lateinit var fecha: String
    private lateinit var hora: String

    private lateinit var etTitular: EditText
    private lateinit var etTarjeta: EditText
    private lateinit var etVencimiento: EditText
    private lateinit var etCvv: EditText
    private lateinit var btnConfirmarPago: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pago)

        citaId = intent.getIntExtra("citaId", -1)
        mascota = intent.getStringExtra("mascota") ?: ""
        motivo = intent.getStringExtra("motivo") ?: ""
        fecha = intent.getStringExtra("fecha") ?: ""
        hora = intent.getStringExtra("hora") ?: ""
        costo = intent.getDoubleExtra("costo", 350.0)

        if (citaId == -1) {
            Toast.makeText(this, "Cita inválida", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        etTitular = findViewById(R.id.etTitular)
        etTarjeta = findViewById(R.id.etTarjeta)
        etVencimiento = findViewById(R.id.etVencimiento)
        etCvv = findViewById(R.id.etCvv)
        btnConfirmarPago = findViewById(R.id.btnConfirmarPago)

        etTarjeta.addTextChangedListener(object : TextWatcher {

            private var editando = false

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {}

            override fun afterTextChanged(s: Editable?) {

                if (editando) return

                editando = true

                val limpio = s.toString()
                    .replace(" ", "")

                val nuevo = StringBuilder()

                limpio.forEachIndexed { index, c ->

                    if (index > 0 && index % 4 == 0) {
                        nuevo.append(" ")
                    }

                    nuevo.append(c)
                }

                etTarjeta.setText(nuevo.toString())

                etTarjeta.setSelection(
                    etTarjeta.text.length
                )

                editando = false

            }

        })

        etVencimiento.addTextChangedListener(object : TextWatcher {

            private var editando = false

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {}

            override fun afterTextChanged(s: Editable?) {

                if (editando || s == null) return

                editando = true

                var limpio = s.toString()
                    .replace("/", "")
                    .filter { it.isDigit() }

                if (limpio.length > 4) {
                    limpio = limpio.substring(0, 4)
                }

                if (limpio.length >= 2) {
                    val mes = limpio.substring(0, 2).toIntOrNull()

                    if (mes == null || mes !in 1..12) {
                        etVencimiento.setText("")
                        Toast.makeText(
                            this@PagoActivity,
                            "Mes inválido. Usa 01 a 12",
                            Toast.LENGTH_SHORT
                        ).show()
                        editando = false
                        return
                    }
                }

                val formateado =
                    if (limpio.length > 2) {
                        limpio.substring(0, 2) + "/" + limpio.substring(2)
                    } else {
                        limpio
                    }

                etVencimiento.setText(formateado)
                etVencimiento.setSelection(etVencimiento.text.length)

                editando = false
            }
        })

        findViewById<TextView>(R.id.tvResumenPago).text =
            "Mascota: $mascota\n" +
                    "Servicio: $motivo\n" +
                    "Fecha: $fecha a las $hora\n" +
                    "Monto: $${String.format(Locale.getDefault(), "%.2f", costo)}"

        btnConfirmarPago.setOnClickListener {
            validarYConfirmarPago()
        }
    }

    private fun validarYConfirmarPago() {

        val titular = etTitular.text.toString().trim()
        val tarjeta = etTarjeta.text.toString().trim()
        val vencimiento = etVencimiento.text.toString().trim()
        val cvv = etCvv.text.toString().trim()

        val tarjetaLimpia = tarjeta.replace(" ", "")

        if (titular.isEmpty()) {

            Toast.makeText(
                this,
                "Ingresa el nombre del titular",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if (tarjetaLimpia.length != 16) {

            Toast.makeText(
                this,
                "La tarjeta debe tener 16 dígitos",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if (
            !vencimiento.matches(
                Regex("(0[1-9]|1[0-2])/[0-9]{2}")
            )
        ) {

            Toast.makeText(
                this,
                "Fecha inválida. Usa MM/AA",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if (cvv.length != 3) {

            Toast.makeText(
                this,
                "El CVV debe tener 3 dígitos",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        registrarPago()
    }

    private fun registrarPago() {
        btnConfirmarPago.isEnabled = false
        btnConfirmarPago.text = "Procesando..."

        lifecycleScope.launch {
            try {
                val referencia = generarReferencia()

                val pagoJson = JSONObject().apply {
                    put("pago_monto", costo)
                    put(
                        "pago_fecha",
                        SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                        ).format(Date())
                    )
                    put("pago_estado", "Pagado")
                    put("pago_conc", motivo)
                    put("cita_id", citaId)
                    put("pago_metodo", "Tarjeta")
                    put("pago_referencia", referencia)
                }

                HttpClientProvider.client.post(
                    "${SupabaseConfig.URL}/rest/v1/pagos"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(
                            HttpHeaders.Authorization,
                            "Bearer ${SupabaseConfig.API_KEY}"
                        )
                        append(HttpHeaders.ContentType, "application/json")
                        append("Prefer", "return=minimal")
                    }
                    setBody(pagoJson.toString())
                }

                val citaJson = JSONObject().apply {
                    put("cita_pagada", true)
                }

                HttpClientProvider.client.patch(
                    "${SupabaseConfig.URL}/rest/v1/citas?cita_id=eq.$citaId"
                ) {
                    headers {
                        append("apikey", SupabaseConfig.API_KEY)
                        append(
                            HttpHeaders.Authorization,
                            "Bearer ${SupabaseConfig.API_KEY}"
                        )
                        append(HttpHeaders.ContentType, "application/json")
                        append("Prefer", "return=minimal")
                    }
                    setBody(citaJson.toString())
                }

                val archivoPdf = generarComprobantePdf(referencia)

                AlertDialog.Builder(this@PagoActivity)
                    .setTitle("Pago realizado")
                    .setMessage(
                        "Referencia: $referencia\n\n" +
                                "El comprobante PDF fue guardado en Descargas."
                    )
                    .setPositiveButton("Aceptar") { _, _ ->
                        finish()
                    }
                    .show()

            } catch (e: Exception) {
                btnConfirmarPago.isEnabled = true
                btnConfirmarPago.text = "Confirmar pago"

                Toast.makeText(
                    this@PagoActivity,
                    "Error al registrar pago: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun generarReferencia(): String {
        val fechaHora = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        return "VT-$fechaHora"
    }

    private fun generarComprobantePdf(referencia: String): File {

        val pdfDocument = PdfDocument()

        val pageInfo = PdfDocument.PageInfo.Builder(
            612,
            1008,
            1
        ).create()

        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titulo = Paint().apply {
            textSize = 24f
            isFakeBoldText = true
        }

        val subtitulo = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }

        val texto = Paint().apply {
            textSize = 14f
        }

        val linea = Paint().apply {
            strokeWidth = 2f
        }

        val bitmap = BitmapFactory.decodeResource(
            resources,
            R.drawable.logo_vetapp
        )

        val logo = Bitmap.createScaledBitmap(
            bitmap,
            150,
            150,
            true
        )

        val xLogo = (pageInfo.pageWidth - logo.width) / 2f

        canvas.drawBitmap(
            logo,
            xLogo,
            25f,
            null
        )

        var y = 210

        val tituloTexto = "VetApp"
        val xTitulo = (pageInfo.pageWidth - titulo.measureText(tituloTexto)) / 2

        canvas.drawText(
            tituloTexto,
            xTitulo,
            y.toFloat(),
            titulo
        )

        y += 35

        val subtituloTexto = "Comprobante de Pago"
        val xSubtitulo =
            (pageInfo.pageWidth - subtitulo.measureText(subtituloTexto)) / 2

        canvas.drawText(
            subtituloTexto,
            xSubtitulo,
            y.toFloat(),
            subtitulo
        )

        y += 40

        canvas.drawLine(
            45f,
            y.toFloat(),
            567f,
            y.toFloat(),
            linea
        )

        y += 40

        canvas.drawText("Referencia: $referencia", 55f, y.toFloat(), texto)
        y += 30

        val fechaActual = SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        ).format(Date())

        canvas.drawText("Fecha de emisión: $fechaActual", 55f, y.toFloat(), texto)
        y += 30

        canvas.drawText("Mascota: $mascota", 55f, y.toFloat(), texto)
        y += 30

        canvas.drawText("Servicio: $motivo", 55f, y.toFloat(), texto)
        y += 30

        canvas.drawText("Fecha de cita: $fecha a las $hora", 55f, y.toFloat(), texto)
        y += 30

        canvas.drawText(
            "Monto pagado: $${String.format(Locale.getDefault(), "%.2f", costo)}",
            55f,
            y.toFloat(),
            texto
        )

        y += 30

        canvas.drawText("Método de pago: Tarjeta", 55f, y.toFloat(), texto)
        y += 30

        canvas.drawText("Estado: PAGADO", 55f, y.toFloat(), texto)

        y += 45

        canvas.drawLine(
            45f,
            y.toFloat(),
            567f,
            y.toFloat(),
            linea
        )

        val pieY = pageInfo.pageHeight - 70

        canvas.drawText(
            "Documento generado automáticamente por VetApp",
            45f,
            pieY.toFloat(),
            texto
        )

        canvas.drawText(
            "Gracias por confiar en VetApp",
            45f,
            (pieY + 22).toFloat(),
            texto
        )

        pdfDocument.finishPage(page)

        val carpeta = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )

        if (!carpeta.exists()) {
            carpeta.mkdirs()
        }

        val archivo = File(
            carpeta,
            "Comprobante_$referencia.pdf"
        )

        pdfDocument.writeTo(FileOutputStream(archivo))
        pdfDocument.close()

        return archivo
    }
}