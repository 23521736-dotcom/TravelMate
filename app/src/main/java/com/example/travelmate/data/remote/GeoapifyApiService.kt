package com.example.travelmate.data.remote

import com.example.travelmate.data.model.GeoapifySearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GeoapifyApiService {

    @GET("v1/geocode/search")
    suspend fun searchPlaces(
        @Query("text") text: String,
        @Query("apiKey") apiKey: String,
        @Query("limit") limit: Int = 20,
        @Query("lang") lang: String = "vi"
    ): GeoapifySearchResponse

    @GET("v1/geocode/autocomplete")
    suspend fun autocompletePlaces(
        @Query("text") text: String,
        @Query("apiKey") apiKey: String,
        @Query("limit") limit: Int = 20,
        @Query("lang") lang: String = "vi"
    ): GeoapifySearchResponse

    @GET("v2/place-details")
    suspend fun getPlaceDetails(
        @Query("id") id: String,
        @Query("apiKey") apiKey: String,
        @Query("lang") lang: String = "vi"
    ): GeoapifySearchResponse
}
