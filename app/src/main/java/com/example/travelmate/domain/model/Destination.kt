package com.example.travelmate.domain.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.PropertyName
import kotlinx.serialization.Serializable

@Serializable
data class Destination(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("location") @set:PropertyName("location") var location: String = "",
    @get:PropertyName("country") @set:PropertyName("country") var country: String = "",
    @get:PropertyName("description") @set:PropertyName("description") var description: String = "",
    @get:PropertyName("rating") @set:PropertyName("rating") var rating: Double = 0.0,
    @get:PropertyName("reviewCount") @set:PropertyName("reviewCount") var reviewCount: Int = 0,
    @get:PropertyName("categoryTags") @set:PropertyName("categoryTags") var categoryTags: List<String> = emptyList(),
    @get:PropertyName("imageUrls") @set:PropertyName("imageUrls") var imageUrls: List<String> = emptyList(),
    @get:PropertyName("isUnesco") @set:PropertyName("isUnesco") var unesco: Boolean = false,
    @get:PropertyName("priceLevel") @set:PropertyName("priceLevel") var priceLevel: Int = 2,
    @get:PropertyName("latitude") @set:PropertyName("latitude") var latitude: Double = 0.0,
    @get:PropertyName("longitude") @set:PropertyName("longitude") var longitude: Double = 0.0,
    @get:PropertyName("isPromoted") @set:PropertyName("isPromoted") var promoted: Boolean = false,
    @get:PropertyName("avgPricePerDay") @set:PropertyName("avgPricePerDay") var avgPricePerDay: Double = 0.0
) {
    @get:Exclude
    val isUnesco: Boolean get() = unesco

    @get:Exclude
    val isPromoted: Boolean get() = promoted
    @get:Exclude
    val categories: List<Category>
        get() = categoryTags.mapNotNull { 
            try { Category.valueOf(it) } catch(e: Exception) { null } 
        }
}

data class Review(
    val id: String = "",
    val userId: String = "",
    val destinationId: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val imageUrls: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
