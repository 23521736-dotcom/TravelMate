package com.example.travelmate.domain.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.PropertyName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Destination(
    @PropertyName("id") var id: String = "",
    @PropertyName("name") var name: String = "",
    @PropertyName("location") var location: String = "",
    @PropertyName("country") var country: String = "",
    @PropertyName("description") var description: String = "",
    @PropertyName("rating") var rating: Double = 0.0,
    @PropertyName("reviewCount") var reviewCount: Int = 0,
    @PropertyName("categoryTags") var categoryTags: List<String> = emptyList(),
    @PropertyName("imageUrls") var imageUrls: List<String> = emptyList(),
    @PropertyName("coverImage") var coverImage: String? = null,
    @SerialName("isUnesco") @get:PropertyName("isUnesco") @set:PropertyName("isUnesco") var unesco: Boolean = false,
    @PropertyName("priceLevel") var priceLevel: Int = 2,
    @PropertyName("latitude") var latitude: Double = 0.0,
    @PropertyName("longitude") var longitude: Double = 0.0,
    @SerialName("isPromoted") @get:PropertyName("isPromoted") @set:PropertyName("isPromoted") var promoted: Boolean = false,
    @PropertyName("avgPricePerDay") var avgPricePerDay: Double = 0.0
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
