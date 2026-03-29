package com.ares.ewe_man.data.remote.api

import com.ares.ewe_man.data.remote.model.GoogleDirectionsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleDirectionsApi {

    @GET("directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") key: String,
        @Query("mode") mode: String = "driving",
        @Query("language") language: String = "es"
    ): Response<GoogleDirectionsResponse>
}
