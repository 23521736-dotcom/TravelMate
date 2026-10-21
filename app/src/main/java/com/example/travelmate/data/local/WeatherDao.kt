package com.example.travelmate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateWeather(weather: WeatherForecastEntity): Long

    @Query("SELECT * FROM weather_forecasts WHERE destinationId = :destinationId")
    fun getWeatherByDestinationId(destinationId: String): Flow<WeatherForecastEntity?>

    @Query("SELECT * FROM weather_forecasts WHERE destinationId = :destinationId")
    fun getWeatherByDestinationIdSync(destinationId: String): WeatherForecastEntity?
}
