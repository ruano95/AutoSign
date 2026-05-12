package com.cyberfrog.AutoSign

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("gt/portal/ws/clockings_save")
    fun fichar(@Body request: ClockingRequest): Call<Map<String, Any>>
}