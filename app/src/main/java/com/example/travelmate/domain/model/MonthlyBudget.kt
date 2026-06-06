package com.example.travelmate.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MonthlyBudget(
    val userId: String = "",
    val month: String = "", // Format: "yyyy-MM"
    val amount: Double = 0.0,
    val thresholdPercent: Int = 80,
    val hasNotifiedThreshold: Boolean = false
)
