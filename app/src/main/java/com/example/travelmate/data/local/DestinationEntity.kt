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
    val cacheTimestamp: Long
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
}
