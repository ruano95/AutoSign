package com.cyberfrog.AutoSign

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    // =========================
    // 🔐 PREFERENCIAS (estado auto ON/OFF)
    // =========================
    private lateinit var prefs: android.content.SharedPreferences

    private fun isAutoEnabled(): Boolean {
        return prefs.getBoolean("auto_enabled", true)
    }

    private fun setAutoEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_enabled", enabled).apply()
    }

    // =========================
    // 🛑 CANCELAR WORKERS
    // =========================
    private fun cancelarFichajes() {
        WorkManager.getInstance(this).cancelUniqueWork("entrada")
        WorkManager.getInstance(this).cancelUniqueWork("salida")
    }

    // =========================
    // 🚀 ON CREATE (ENTRY POINT UI)
    // =========================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔐 inicializar prefs (IMPORTANTE)
        prefs = getSharedPreferences("clocking_prefs", MODE_PRIVATE)

        // 📱 UI
        val button = findViewById<Button>(R.id.btnFichar)
        val resultText = findViewById<TextView>(R.id.txtResultado)
        val btnToggle = findViewById<Button>(R.id.btnToggleAuto)

        // 🟢 botón fichar manual
        button.setOnClickListener {
            fichar(resultText)
        }

        // 🔁 toggle auto fichaje (ESTO ESTABA MAL COLOCADO ANTES)
        btnToggle.setOnClickListener {
            val enabled = !isAutoEnabled()
            setAutoEnabled(enabled)

            if (enabled) programarFichajes() else cancelarFichajes()

            updateToggleUI(btnToggle, enabled)
        }

        // 🚀 programación inicial
        programarFichajes()
    }

    // =========================
    // 🧾 FICHAR MANUAL
    // =========================
    private fun fichar(resultText: TextView) {

        val request = ClockingRequest(
            sessionInfo = SessionInfo(
                user = BuildConfig.USER_NAME,
                password = BuildConfig.USER_PASSWORD
            ),
            clientTime = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss"
            ).format(java.util.Date())
        )

        RetrofitClient.api.fichar(request)
            .enqueue(object : Callback<Map<String, Any>> {

                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {
                    val body = response.body()

                    resultText.text = if (body != null) {
                        formatClockings(body)
                    } else {
                        "Sin respuesta"
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    resultText.text = "ERROR:\n${t.message}"
                }
            })
    }

    // =========================
    // ⏱ PROGRAMAR FICHAJES AUTOMÁTICOS
    // =========================
    private fun programarFichajes() {

        val entrada = PeriodicWorkRequestBuilder<ClockingWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(calcularDelayVariable(9, 0, 13), TimeUnit.MILLISECONDS)
            .build()

        val salida = PeriodicWorkRequestBuilder<ClockingWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(calcularDelayVariable(18, 30, 11), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "entrada",
            ExistingPeriodicWorkPolicy.REPLACE,
            entrada
        )

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "salida",
            ExistingPeriodicWorkPolicy.REPLACE,
            salida
        )
    }

    // =========================
    // 🎲 DELAY CON VARIACIÓN
    // =========================
    private fun calcularDelayVariable(hour: Int, minute: Int, variacionMinutos: Int): Long {

        val now = java.time.LocalDateTime.now()

        val randomOffset = (-variacionMinutos..variacionMinutos).random()

        var target = now
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .plusMinutes(randomOffset.toLong())

        if (target.isBefore(now)) {
            target = target.plusDays(1)
        }

        return java.time.Duration.between(now, target).toMillis()
    }

    // =========================
    // 📊 FORMATEO DE RESPUESTA
    // =========================
    private fun formatClockings(body: Map<String, Any>): String {

        val data = body["data"] as? Map<*, *> ?: return "Sin datos"
        val list = data["recentClockings"] as? List<*> ?: return "Sin fichajes"

        val grouped = list.mapNotNull { item: Any? ->

            val row = item as? List<*> ?: return@mapNotNull null
            val rawDate = row[0] as? String ?: return@mapNotNull null

            val datePart = rawDate.take(10)
            val timePart = rawDate.takeIf { it.length >= 16 }?.substring(11, 16) ?: return@mapNotNull null

            Triple(datePart, timePart, rawDate)
        }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )
            .toSortedMap(compareByDescending { it })

        return buildString {

            grouped.forEach { (date, times) ->

                val prettyDate = formatDate(date)

                append("📅 $prettyDate\n")

                times.sorted().forEach {
                    append("• $it\n")
                }

                append("\n")
            }
        }
    }

    // =========================
    // 📅 FORMATEO FECHA
    // =========================
    private fun formatDate(date: String): String {

        return try {
            val parts = date.split("-")
            val day = parts[2]
            val month = parts[1]

            val monthName = when (month) {
                "01" -> "enero"
                "02" -> "febrero"
                "03" -> "marzo"
                "04" -> "abril"
                "05" -> "mayo"
                "06" -> "junio"
                "07" -> "julio"
                "08" -> "agosto"
                "09" -> "septiembre"
                "10" -> "octubre"
                "11" -> "noviembre"
                "12" -> "diciembre"
                else -> month
            }

            "$day $monthName"
        } catch (e: Exception) {
            date
        }
    }
}

// =========================
// 📅 COLOR DINÁMICO DEL BOTÓN DE AUTO ON/OFF
// =========================
private fun updateToggleUI(button: Button, enabled: Boolean) {
    button.text = if (enabled) "Auto: ON" else "Auto: OFF"

    val color = if (enabled) {
        android.graphics.Color.parseColor("#4CAF50") // verde
    } else {
        android.graphics.Color.parseColor("#F44336") // rojo
    }

    button.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
}