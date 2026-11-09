package com.example.travelmate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val tripId: String,
    val amount: Double,
    val currency: String,
    val category: String,
    val description: String,
    val date: Long,
    val activityId: String?,

    // Offline Sync flags
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
