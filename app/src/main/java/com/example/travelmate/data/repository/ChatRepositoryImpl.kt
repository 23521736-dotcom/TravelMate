package com.example.travelmate.data.repository

import com.example.travelmate.data.model.GroqChatRequest
import com.example.travelmate.data.model.GroqMessage
import com.example.travelmate.data.remote.GroqApiService
import com.example.travelmate.domain.repository.ChatRepository
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val apiService: GroqApiService
) : ChatRepository {

    // Using a newer, stable model
    private val modelId = "llama-3.3-70b-versatile"
    private val apiKey = "Bearer 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"

    override suspend fun getChatResponse(messages: List<GroqMessage>): Result<GroqMessage> {
        return try {
            val response = apiService.getChatCompletion(
                apiKey = apiKey,
                request = GroqChatRequest(
                    messages = messages,
                    model = modelId,
                    maxTokens = 2048,
                    temperature = 0.7
                )
            )
            val assistantMessage = response.choices.firstOrNull()?.message
            if (assistantMessage != null) {
                Result.success(assistantMessage)
            } else {
                Result.failure(Exception("AI không phản hồi dữ liệu phù hợp"))
            }
        } catch (e: Exception) {
            // Better error message for the user
            val displayMessage = when {
                e.message?.contains("400") == true -> "Lỗi yêu cầu (HTTP 400). Vui lòng thử lại hoặc rút ngắn tin nhắn."
                e.message?.contains("401") == true -> "Lỗi xác thực API Key."
                e.message?.contains("429") == true -> "Bạn đã vượt quá giới hạn lượt dùng thử. Vui lòng chờ một lát."
                else -> e.message ?: "Đã xảy ra lỗi khi kết nối với AI"
            }
            Result.failure(Exception(displayMessage))
        }
    }
}
