package com.example.travelmate.domain.model

import com.google.firebase.database.PropertyName
import kotlinx.serialization.Serializable

@Serializable
data class TripMember(
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    @get:PropertyName("tripId") @set:PropertyName("tripId") var tripId: String = "",
    @get:PropertyName("role") @set:PropertyName("role") var role: String = ROLE_VIEWER,
    @get:PropertyName("status") @set:PropertyName("status") var status: String = STATUS_JOINED,
    @get:PropertyName("joinedAt") @set:PropertyName("joinedAt") var joinedAt: Long = System.currentTimeMillis(),
    // Cached user info for faster UI rendering without extra lookups
    @get:PropertyName("userName") @set:PropertyName("userName") var userName: String = "",
    @get:PropertyName("userPhotoUrl") @set:PropertyName("userPhotoUrl") var userPhotoUrl: String? = null
) {
    companion object {
        const val ROLE_OWNER = "OWNER"
        const val ROLE_EDITOR = "EDITOR"
        const val ROLE_VIEWER = "VIEWER"

        const val STATUS_INVITED = "INVITED"
        const val STATUS_JOINED = "JOINED"
        const val STATUS_LEFT = "LEFT"
    }
}
