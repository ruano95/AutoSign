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

        val request = ClockingRequest(
            sessionInfo = SessionInfo(
                user = "11859304K",
                password = "Ust12345$"
            ),
            clientTime = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.getDefault()
            ).format(Date())
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