package com.example.travelmate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable

@Serializable
data class DayForecastModel(
    val date: String,
    val minTemp: Double,
    val maxTemp: Double,
    val weatherCode: Int
)

@Entity(tableName = "weather_forecasts")
@TypeConverters(Converters::class)
data class WeatherForecastEntity(
    @PrimaryKey
    val destinationId: String,
    val currentTemp: Double,
    val currentWeatherCode: Int,
    val humidity: Int,
    val forecastDays: List<DayForecastModel>,
    val lastUpdated: Long = System.currentTimeMillis()
)
