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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.btnFichar)
        val resultText = findViewById<TextView>(R.id.txtResultado)

        button.setOnClickListener {
            fichar(resultText)
        }

        programarFichajes()
    }

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

                    if (body != null) {
                        resultText.text = formatClockings(body)
                    } else {
                        resultText.text = "Sin respuesta"
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    resultText.text = "ERROR:\n${t.message}"
                }
            })
    }

    private fun programarFichajes() {

        val entrada = PeriodicWorkRequestBuilder<ClockingWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(calcularDelay(9, 0), TimeUnit.MILLISECONDS)
            .build()

        val salida = PeriodicWorkRequestBuilder<ClockingWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(calcularDelay(18, 0), TimeUnit.MILLISECONDS)
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

    private fun calcularDelay(hour: Int, minute: Int): Long {

        val now = java.time.LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0)

        if (target.isBefore(now)) {
            target = target.plusDays(1)
        }

        return java.time.Duration.between(now, target).toMillis()
    }

    private fun formatClockings(body: Map<String, Any>): String {

        val data = body["data"] as? Map<*, *> ?: return "Sin datos"
        val list = data["recentClockings"] as? List<*> ?: return "Sin fichajes"

        // Agrupar por día
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

                // 👉 ORDENAR HORAS AQUÍ
                times.sorted().forEach {
                    append("• $it\n")
                }

                append("\n")
            }
        }
    }

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