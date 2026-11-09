package com.example.travelmate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DestinationDao {
    @Query("SELECT * FROM destinations ORDER BY rating DESC, reviewCount DESC LIMIT 20")
    fun getTopDestinations(): Flow<List<DestinationEntity>>

    @Query("SELECT * FROM destinations ORDER BY rating DESC, reviewCount DESC LIMIT 20")
    fun getTopDestinationsSync(): List<DestinationEntity>

    @Query("SELECT * FROM destinations")
    fun getAllDestinations(): Flow<List<DestinationEntity>>

    @Query("SELECT * FROM destinations")
    fun getAllDestinationsSync(): List<DestinationEntity>

    @Query("SELECT * FROM destinations WHERE id = :id")
    fun getDestinationById(id: String): Flow<DestinationEntity?>

    @Query("SELECT * FROM destinations WHERE id = :id")
    fun getDestinationByIdSync(id: String): DestinationEntity?

    @Query("SELECT * FROM destinations WHERE name LIKE '%' || :searchQuery || '%' OR location LIKE '%' || :searchQuery || '%'")
    fun searchDestinations(searchQuery: String): Flow<List<DestinationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDestinations(destinations: List<DestinationEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDestination(destination: DestinationEntity): Long

    @Query("DELETE FROM destinations")
    fun deleteAllDestinations(): Int

    @Query("UPDATE destinations SET wikiDescription = :desc, wikiArticleUrl = :url, wikiLastUpdated = :timestamp WHERE id = :id")
    fun updateWikiInfo(id: String, desc: String?, url: String?, timestamp: Long): Int

    @Query("SELECT * FROM destinations WHERE isSavedOffline = 1 ORDER BY name ASC")
    fun getSavedOfflineDestinations(): Flow<List<DestinationEntity>>

    @Query("UPDATE destinations SET isSavedOffline = :isSaved WHERE id = :id")
    fun updateOfflineSavedStatus(id: String, isSaved: Boolean): Int
}
