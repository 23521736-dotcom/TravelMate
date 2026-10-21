package com.example.travelmate.data.model

import com.example.travelmate.data.local.DestinationEntity
import com.example.travelmate.domain.model.Destination

fun DestinationEntity.toDomainModel(): Destination {
    return Destination(
        id = this.id,
        name = this.name,
        location = this.location,
        country = this.country,
        description = this.description,
        rating = this.rating,
        reviewCount = this.reviewCount,
        categoryTags = this.categoryTags,
        imageUrls = this.imageUrls,
        coverImage = this.coverImage,
        unesco = this.isUnesco,
        priceLevel = this.priceLevel,
        latitude = this.latitude,
        longitude = this.longitude,
        promoted = this.isPromoted,
        avgPricePerDay = this.avgPricePerDay
    )
}

fun Destination.toEntity(): DestinationEntity {
    return DestinationEntity(
        id = this.id,
        name = this.name,
        location = this.location,
        country = this.country,
        description = this.description,
        rating = this.rating,
        reviewCount = this.reviewCount,
        categoryTags = this.categoryTags,
        imageUrls = this.imageUrls,
        coverImage = this.coverImage,
        isUnesco = this.unesco,
        priceLevel = this.priceLevel,
        latitude = this.latitude,
        longitude = this.longitude,
        isPromoted = this.promoted,
        avgPricePerDay = this.avgPricePerDay,
        cacheTimestamp = System.currentTimeMillis()
    )
}
