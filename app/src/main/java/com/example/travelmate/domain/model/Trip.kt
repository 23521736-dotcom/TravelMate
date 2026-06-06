package com.example.travelmate.domain.model

import com.google.firebase.database.PropertyName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Trip(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("destination") @set:PropertyName("destination") var destination: String = "",
    @get:PropertyName("destinationId") @set:PropertyName("destinationId") var destinationId: String = "",
    @get:PropertyName("description") @set:PropertyName("description") var description: String = "",
    @get:PropertyName("startDate") @set:PropertyName("startDate") var startDate: Long = 0,
    @get:PropertyName("endDate") @set:PropertyName("endDate") var endDate: Long = 0,
    @get:PropertyName("durationDays") @set:PropertyName("durationDays") var durationDays: Int = 0,
    @get:PropertyName("budgetAmount") @set:PropertyName("budgetAmount") var budgetAmount: Double = 0.0,
    @get:PropertyName("budgetLevel") @set:PropertyName("budgetLevel") var budgetLevel: String = "Mid-range",
    @get:PropertyName("currency") @set:PropertyName("currency") var currency: String = "VND",
    @get:PropertyName("status") @set:PropertyName("status") var status: String = STATUS_UPCOMING, 
    @get:PropertyName("coverImageUrl") @set:PropertyName("coverImageUrl") var coverImageUrl: String = "",
    @get:PropertyName("galleryUrls") @set:PropertyName("galleryUrls") var galleryUrls: List<String> = emptyList(),
    @get:PropertyName("itinerary") @set:PropertyName("itinerary") var itinerary: List<ItineraryDay> = emptyList(),
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_UPCOMING = "upcoming"
        const val STATUS_ONGOING = "ongoing"
        const val STATUS_COMPLETED = "completed"

        fun getTripStatus(startDate: Long, endDate: Long): String {
            val now = System.currentTimeMillis()
            return when {
                now < startDate -> STATUS_UPCOMING
                now in startDate..endDate -> STATUS_ONGOING
                else -> STATUS_COMPLETED
            }
        }
    }

    fun getCalculatedStatus(): String = getTripStatus(startDate, endDate)
}

@Serializable
data class ItineraryDay(
    @get:PropertyName("dayNumber") @set:PropertyName("dayNumber") var dayNumber: Int = 1,
    @get:PropertyName("date") @set:PropertyName("date") var date: Long = 0,
    @get:PropertyName("activities") @set:PropertyName("activities") var activities: List<TripActivity> = emptyList()
)

@Serializable
data class TripActivity(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("time") @set:PropertyName("time") var time: String = "",
    @get:PropertyName("title") @set:PropertyName("title") var title: String = "",
    @get:PropertyName("location") @set:PropertyName("location") var location: String = "",
    @get:PropertyName("note") @set:PropertyName("note") var note: String = "",
    @get:PropertyName("type") @set:PropertyName("type") var type: String = "Sightseeing", // Sightseeing, Food, Activity, Transport, Shopping, Leisure, Stay
    @SerialName("isDone") @get:PropertyName("isDone") @set:PropertyName("isDone") var done: Boolean = false,
    @get:PropertyName("linkedExpenseId") @set:PropertyName("linkedExpenseId") var linkedExpenseId: String? = null
) {
    @get:com.google.firebase.database.Exclude
    val isDone: Boolean get() = done
}
