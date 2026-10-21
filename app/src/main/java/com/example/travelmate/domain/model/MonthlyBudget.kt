package com.example.travelmate.domain.model

import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable

@Serializable
data class MonthlyBudget(
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    @get:PropertyName("month") @set:PropertyName("month") var month: String = "", // Format: "yyyy-MM"
    @get:PropertyName("amount") @set:PropertyName("amount") var amount: Double = 0.0,
    @get:PropertyName("thresholdPercent") @set:PropertyName("thresholdPercent") var thresholdPercent: Int = 80,
    @get:PropertyName("hasNotifiedThreshold") @set:PropertyName("hasNotifiedThreshold") var hasNotifiedThreshold: Boolean = false
)
