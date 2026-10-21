package com.example.travelmate.domain.model

import com.google.firebase.firestore.Exclude
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String = "",
    val email: String = "",
    @get:Exclude val password: String = "",
    val fullName: String? = null,
    val photoUrl: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val displayName: String? = null,
    val isBlocked: Boolean = false,
    val blockedReason: String? = null,
    val totalTrips: Int = 0,
    val totalSpent: Double = 0.0,
    val upcomingTrips: Int = 0,
    val interests: List<Category> = emptyList(),
    val wishlistIds: List<String> = emptyList()
)
