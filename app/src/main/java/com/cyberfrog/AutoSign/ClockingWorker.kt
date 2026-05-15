package com.cyberfrog.AutoSign

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Date

class ClockingWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {

        // =========================
        // 📅 Día actual
        // =========================
        val today = LocalDate.now().dayOfWeek

        // =========================
        // ⚙️ Preferencias (auto ON/OFF)
        // =========================
        val prefs = applicationContext.getSharedPreferences(
            "clocking_prefs",
            Context.MODE_PRIVATE
        )

        val enabled = prefs.getBoolean("auto_enabled", true)

        if (!enabled) {
            return Result.success()
        }

        // =========================
        // 🚫 Bloqueo fines de semana
        // =========================
        if (today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY) {
            return Result.success()
        }

        // =========================
        // 🧾 Construcción request
        // =========================
        val request = ClockingRequest(
            sessionInfo = SessionInfo(
                user = BuildConfig.USER_NAME,
                password = BuildConfig.USER_PASSWORD
            ),
            clientTime = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss"
            ).format(Date())
        )

        return try {

            // =========================
            // 🌐 llamada síncrona (IMPORTANTE en Worker)
            // =========================
            val response = RetrofitClient.api.fichar(request).execute()

            if (response.isSuccessful) {

                // 🔍 opcional: puedes loguear aquí respuesta
                Result.success()

            } else {

                // ❗ fallo HTTP (400/500/etc)
                Result.retry()
            }

        } catch (e: Exception) {

            e.printStackTrace()

            // 🔁 fallo de red / parseo
            Result.retry()
        }
    }
}