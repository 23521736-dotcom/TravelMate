package com.example.travelmate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE destinationId = :destinationId ORDER BY createdAt DESC")
    fun getReviewsForDestination(destinationId: String): List<ReviewEntity>

    @Query("SELECT * FROM reviews WHERE userId = :userId ORDER BY createdAt DESC")
    fun getReviewsForUser(userId: String): List<ReviewEntity>

    @Query("SELECT * FROM reviews WHERE syncStatus != 'SYNCED'")
    fun getPendingSyncReviews(): List<ReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReview(review: ReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReviews(reviews: List<ReviewEntity>)

    @Query("UPDATE reviews SET syncStatus = :status WHERE id = :reviewId")
    fun updateSyncStatus(reviewId: String, status: SyncStatus)

    @Query("DELETE FROM reviews WHERE id = :reviewId")
    fun deleteReview(reviewId: String)
}
