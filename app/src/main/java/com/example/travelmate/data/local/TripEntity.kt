package com.example.travelmate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.travelmate.domain.model.ItineraryDay

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val destination: String,
    val destinationId: String,
    val description: String,
    val startDate: Long,
    val endDate: Long,
    val durationDays: Int,
    val budgetAmount: Double,
    val budgetLevel: String,
    val currency: String,
    val status: String,
    val coverImageUrl: String,
    val galleryUrls: List<String>,
    val itinerary: List<ItineraryDay>,
    val visibility: String,
    val shareToken: String,
    val inviteCode: String,
    val createdAt: Long,
    
    // Offline Sync flags
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
