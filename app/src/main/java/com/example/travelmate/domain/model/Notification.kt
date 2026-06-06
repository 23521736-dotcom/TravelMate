package com.example.travelmate.domain.model

import com.google.firebase.database.PropertyName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    @get:PropertyName("title") @set:PropertyName("title") var title: String = "",
    @get:PropertyName("message") @set:PropertyName("message") var message: String = "",
    @get:PropertyName("type") @set:PropertyName("type") var type: String = "info", // budget, trip_start, review, welcome, system
    @get:PropertyName("timestamp") @set:PropertyName("timestamp") var timestamp: Long = System.currentTimeMillis(),
    @SerialName("isRead") @get:PropertyName("isRead") @set:PropertyName("isRead") var read: Boolean = false,
    @get:PropertyName("tripId") @set:PropertyName("tripId") var tripId: String? = null,
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl") var imageUrl: String? = null,
    @get:PropertyName("actionUrl") @set:PropertyName("actionUrl") var actionUrl: String? = null
) {
    @get:com.google.firebase.database.Exclude
    val isRead: Boolean get() = read
}
