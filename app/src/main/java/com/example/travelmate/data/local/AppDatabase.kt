package com.example.travelmate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        DestinationEntity::class, 
        WeatherForecastEntity::class,
        TripEntity::class,
        ReviewEntity::class,
        ExpenseEntity::class
    ], 
    version = 4, 
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun destinationDao(): DestinationDao
    abstract fun weatherDao(): WeatherDao
    abstract fun tripDao(): TripDao
    abstract fun reviewDao(): ReviewDao
    abstract fun expenseDao(): ExpenseDao
}
