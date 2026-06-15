package com.example.travelmate.domain.repository

import com.example.travelmate.domain.model.Review
import com.example.travelmate.util.NetworkResult
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun getReviewsForDestination(destinationId: String): Flow<NetworkResult<List<Review>>>
    fun getUserReviews(userId: String): Flow<NetworkResult<List<Review>>>
    fun addReview(review: Review): Flow<NetworkResult<Unit>>
    fun deleteReview(destinationId: String, reviewId: String): Flow<NetworkResult<Unit>>
    suspend fun updateUserInReviews(userId: String, newName: String, newPhotoUrl: String?)
}
