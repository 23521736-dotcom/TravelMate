package com.example.travelmate.domain.model

data class Expense(
    val id: String = "",
    val userId: String = "",
    val tripId: String = "",
    val amount: Double = 0.0,
    val currency: String = "VND",
    val category: String = "Other",
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val activityId: String? = null
)
