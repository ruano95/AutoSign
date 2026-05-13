package com.cyberfrog.AutoSign

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class ClockingWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {

        val today = java.time.LocalDate.now().dayOfWeek

        if (today == java.time.DayOfWeek.SATURDAY || today == java.time.DayOfWeek.SUNDAY) {
            return Result.success()
        }

        val request = ClockingRequest(
            sessionInfo = SessionInfo(
                user = BuildConfig.USER_NAME,
                password = BuildConfig.USER_PASSWORD
            ),
            clientTime = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss"
            ).format(java.util.Date())
        )

        try {
            val response = RetrofitClient.api.fichar(request).execute()

            return if (response.isSuccessful) {
                Result.success()
            } else {
                Result.retry()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}