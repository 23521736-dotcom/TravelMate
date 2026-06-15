package com.example.travelmate.domain.model

import com.google.firebase.database.PropertyName
import kotlinx.serialization.Serializable

@Serializable
data class Review(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("destinationId") @set:PropertyName("destinationId") var destinationId: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    @get:PropertyName("userName") @set:PropertyName("userName") var userName: String = "",
    @get:PropertyName("userPhotoUrl") @set:PropertyName("userPhotoUrl") var userPhotoUrl: String? = null,
    @get:PropertyName("rating") @set:PropertyName("rating") var rating: Float = 0f,
    @get:PropertyName("comment") @set:PropertyName("comment") var comment: String = "",
    @get:PropertyName("imageUrls") @set:PropertyName("imageUrls") var imageUrls: List<String> = emptyList(),
    @get:PropertyName("destinationName") @set:PropertyName("destinationName") var destinationName: String = "",
    @get:PropertyName("destinationImage") @set:PropertyName("destinationImage") var destinationImage: String = "",
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Long = System.currentTimeMillis()
)
