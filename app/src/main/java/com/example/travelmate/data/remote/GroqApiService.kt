package com.example.travelmate.data.remote

import com.example.travelmate.data.model.GroqChatRequest
import com.example.travelmate.data.model.GroqChatResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GroqApiService {
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: GroqChatRequest
    ): GroqChatResponse
}
