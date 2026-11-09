package com.example.travelmate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "destinations")
@TypeConverters(Converters::class)
data class DestinationEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val location: String,
    val country: String,
    val description: String,
    val rating: Double,
    val reviewCount: Int,
    val categoryTags: List<String>,
    val imageUrls: List<String>,
    val coverImage: String?,
    val isUnesco: Boolean,
    val priceLevel: Int,
    val latitude: Double,
    val longitude: Double,
    val isPromoted: Boolean,
    val avgPricePerDay: Double,
    val cacheTimestamp: Long = System.currentTimeMillis(),
    val wikiDescription: String? = null,
    val wikiArticleUrl: String? = null,
    val wikiLastUpdated: Long = 0L,
    val isSavedOffline: Boolean = false
)

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromDayForecastList(value: List<DayForecastModel>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toDayForecastList(value: String): List<DayForecastModel> {
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromItineraryDayList(value: List<com.example.travelmate.domain.model.ItineraryDay>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toItineraryDayList(value: String): List<com.example.travelmate.domain.model.ItineraryDay> {
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String {
        return value.name
    }

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return try {
            SyncStatus.valueOf(value)
        } catch (e: Exception) {
            SyncStatus.SYNCED
        }
    }
}
