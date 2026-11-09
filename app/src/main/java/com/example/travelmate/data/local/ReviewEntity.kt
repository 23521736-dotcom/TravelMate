package com.example.travelmate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val destinationId: String,
    val userId: String,
    val userName: String,
    val userPhotoUrl: String?,
    val rating: Float,
    val comment: String,
    val imageUrls: List<String>,
    val destinationName: String,
    val destinationImage: String,
    val createdAt: Long,

    // Offline Sync flags
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    // Store local URIs before uploading to remote server
    val localImageUris: List<String> = emptyList()
)
