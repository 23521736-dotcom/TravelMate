package com.example.travelmate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TripDao {
    @Query("SELECT * FROM trips")
    fun getAllTrips(): List<TripEntity>

    @Query("SELECT * FROM trips WHERE userId = :userId ORDER BY createdAt DESC")
    fun getTripsForUser(userId: String): List<TripEntity>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun getTripById(tripId: String): TripEntity?

    @Query("SELECT * FROM trips WHERE syncStatus != 'SYNCED'")
    fun getPendingSyncTrips(): List<TripEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTrip(trip: TripEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTrips(trips: List<TripEntity>)

    @Query("UPDATE trips SET syncStatus = :status WHERE id = :tripId")
    fun updateSyncStatus(tripId: String, status: SyncStatus)

    @Query("DELETE FROM trips WHERE id = :tripId")
    fun deleteTrip(tripId: String)
}
