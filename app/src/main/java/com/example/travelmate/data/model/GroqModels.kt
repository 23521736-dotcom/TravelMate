package com.example.travelmate.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroqChatRequest(
    val messages: List<GroqMessage>,
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int = 1024,
    val temperature: Double = 0.7,
    @SerialName("top_p") val topP: Double = 1.0,
    val stream: Boolean = false
)

@Serializable
data class GroqMessage(
    val role: String,
    val content: String
)

@Serializable
data class GroqChatResponse(
    val id: String? = null,
    val choices: List<GroqChoice>
)

@Serializable
data class GroqChoice(
    val index: Int? = null,
    val message: GroqMessage,
    @SerialName("finish_reason") val finishReason: String? = null
)
