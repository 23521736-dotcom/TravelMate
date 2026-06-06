package com.example.travelmate.domain.repository

import com.example.travelmate.data.model.GroqMessage

interface ChatRepository {
    suspend fun getChatResponse(messages: List<GroqMessage>): Result<GroqMessage>
}
