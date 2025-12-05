package com.teleconizer.app.data.api

import com.teleconizer.app.data.model.SensorData
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TeleconizerApi {
    
    @GET("api/data")
    suspend fun getLatestData(
        @Query("api_key") apiKey: String
    ): Response<SensorData>
    
    @GET("api/status")
    suspend fun getServerStatus(): Response<Map<String, Any>>
}

