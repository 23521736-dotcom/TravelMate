package com.example.travelmate.domain.model

import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    @get:PropertyName("tripId") @set:PropertyName("tripId") var tripId: String = "",
    @get:PropertyName("amount") @set:PropertyName("amount") var amount: Double = 0.0,
    @get:PropertyName("currency") @set:PropertyName("currency") var currency: String = "VND",
    @get:PropertyName("category") @set:PropertyName("category") var category: String = "Other",
    @get:PropertyName("description") @set:PropertyName("description") var description: String = "",
    @get:PropertyName("date") @set:PropertyName("date") var date: Long = System.currentTimeMillis(),
    @get:PropertyName("activityId") @set:PropertyName("activityId") var activityId: String? = null
)
