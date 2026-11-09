package com.example.travelmate.data.model

import com.example.travelmate.data.local.ExpenseEntity
import com.example.travelmate.data.local.ReviewEntity
import com.example.travelmate.data.local.SyncStatus
import com.example.travelmate.data.local.TripEntity
import com.example.travelmate.domain.model.Expense
import com.example.travelmate.domain.model.Review
import com.example.travelmate.domain.model.Trip

fun TripEntity.toDomainModel(): Trip {
    return Trip(
        id = this.id,
        userId = this.userId,
        name = this.name,
        destination = this.destination,
        destinationId = this.destinationId,
        description = this.description,
        startDate = this.startDate,
        endDate = this.endDate,
        durationDays = this.durationDays,
        budgetAmount = this.budgetAmount,
        budgetLevel = this.budgetLevel,
        currency = this.currency,
        status = this.status,
        coverImageUrl = this.coverImageUrl,
        galleryUrls = this.galleryUrls,
        itinerary = this.itinerary,
        visibility = this.visibility,
        shareToken = this.shareToken,
        inviteCode = this.inviteCode,
        createdAt = this.createdAt
    )
}

fun Trip.toEntity(syncStatus: SyncStatus = SyncStatus.SYNCED): TripEntity {
    return TripEntity(
        id = this.id,
        userId = this.userId,
        name = this.name,
        destination = this.destination,
        destinationId = this.destinationId,
        description = this.description,
        startDate = this.startDate,
        endDate = this.endDate,
        durationDays = this.durationDays,
        budgetAmount = this.budgetAmount,
        budgetLevel = this.budgetLevel,
        currency = this.currency,
        status = this.status,
        coverImageUrl = this.coverImageUrl,
        galleryUrls = this.galleryUrls,
        itinerary = this.itinerary,
        visibility = this.visibility,
        shareToken = this.shareToken,
        inviteCode = this.inviteCode,
        createdAt = this.createdAt,
        syncStatus = syncStatus
    )
}

fun ReviewEntity.toDomainModel(): Review {
    return Review(
        id = this.id,
        destinationId = this.destinationId,
        userId = this.userId,
        userName = this.userName,
        userPhotoUrl = this.userPhotoUrl,
        rating = this.rating,
        comment = this.comment,
        imageUrls = this.imageUrls, // Ignore localImageUris in Domain model for now
        destinationName = this.destinationName,
        destinationImage = this.destinationImage,
        createdAt = this.createdAt
    )
}

fun Review.toEntity(syncStatus: SyncStatus = SyncStatus.SYNCED, localImageUris: List<String> = emptyList()): ReviewEntity {
    return ReviewEntity(
        id = this.id,
        destinationId = this.destinationId,
        userId = this.userId,
        userName = this.userName,
        userPhotoUrl = this.userPhotoUrl,
        rating = this.rating,
        comment = this.comment,
        imageUrls = this.imageUrls,
        destinationName = this.destinationName,
        destinationImage = this.destinationImage,
        createdAt = this.createdAt,
        syncStatus = syncStatus,
        localImageUris = localImageUris
    )
}

fun ExpenseEntity.toDomainModel(): Expense {
    return Expense(
        id = this.id,
        userId = this.userId,
        tripId = this.tripId,
        amount = this.amount,
        currency = this.currency,
        category = this.category,
        description = this.description,
        date = this.date,
        activityId = this.activityId
    )
}

fun Expense.toEntity(syncStatus: SyncStatus = SyncStatus.SYNCED): ExpenseEntity {
    return ExpenseEntity(
        id = this.id,
        userId = this.userId,
        tripId = this.tripId,
        amount = this.amount,
        currency = this.currency,
        category = this.category,
        description = this.description,
        date = this.date,
        activityId = this.activityId,
        syncStatus = syncStatus
    )
}
