package com.example.travelmate.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class UnsplashSearchResponse(
    @SerialName("results") val results: List<UnsplashPhoto> = emptyList()
)

@Serializable
data class UnsplashPhoto(
    @SerialName("urls") val urls: UnsplashUrls? = null
)

@Serializable
data class UnsplashUrls(
    @SerialName("regular") val regular: String? = null,
    @SerialName("small") val small: String? = null,
    @SerialName("full") val full: String? = null
)

@Serializable
data class PixabaySearchResponse(
    @SerialName("hits") val hits: List<PixabayPhoto> = emptyList()
)

@Serializable
data class PixabayPhoto(
    @SerialName("largeImageURL") val largeImageURL: String? = null,
    @SerialName("webformatURL") val webformatURL: String? = null
)

interface ImageSearchApiService {

    @GET("https://api.unsplash.com/search/photos")
    suspend fun searchUnsplash(
        @Query("query") query: String,
        @Query("client_id") clientId: String,
        @Query("orientation") orientation: String = "landscape",
        @Query("per_page") perPage: Int = 1
    ): UnsplashSearchResponse

    @GET("https://pixabay.com/api/")
    suspend fun searchPixabay(
        @Query("q") query: String,
        @Query("key") apiKey: String,
        @Query("image_type") imageType: String = "photo",
        @Query("orientation") orientation: String = "horizontal",
        @Query("per_page") perPage: Int = 3
    ): PixabaySearchResponse
}
