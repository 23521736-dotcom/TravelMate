package com.example.travelmate.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

@Serializable
data class WikipediaSummaryResponse(
    @SerialName("title") val title: String? = null,
    @SerialName("extract") val extract: String? = null,
    @SerialName("thumbnail") val thumbnail: WikipediaThumbnail? = null,
    @SerialName("content_urls") val contentUrls: WikipediaContentUrls? = null
)

@Serializable
data class WikipediaThumbnail(
    @SerialName("source") val source: String? = null
)

@Serializable
data class WikipediaContentUrls(
    @SerialName("desktop") val desktop: WikipediaUrlPage? = null
)

@Serializable
data class WikipediaUrlPage(
    @SerialName("page") val page: String? = null
)

interface WikipediaApiService {

    @GET("https://vi.wikipedia.org/api/rest_v1/page/summary/{title}")
    suspend fun getPageSummary(
        @Path("title") title: String,
        @Header("User-Agent") userAgent: String = "TravelApp/1.0 (contact@travelapp.com)"
    ): WikipediaSummaryResponse
}
