package com.example.travelmate.data.repository

import com.example.travelmate.domain.model.Review
import com.example.travelmate.domain.repository.ReviewRepository
import com.example.travelmate.util.NetworkResult
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID
import javax.inject.Inject

class ReviewRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase
) : ReviewRepository {

    private val reviewsRef = database.getReference("reviews")

    override fun getReviewsForDestination(destinationId: String): Flow<NetworkResult<List<Review>>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val listener = reviewsRef.child(destinationId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val reviews = mutableListOf<Review>()
                    snapshot.children.forEach { child ->
                        child.getValue(Review::class.java)?.let { reviews.add(it) }
                    }
                    trySend(NetworkResult.Success(reviews.sortedByDescending { it.createdAt }))
                } catch (e: Exception) {
                    trySend(NetworkResult.Error("Lỗi nạp đánh giá: ${e.message}"))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(NetworkResult.Error(error.message))
            }
        })
        awaitClose { reviewsRef.child(destinationId).removeEventListener(listener) }
    }

    override fun addReview(review: Review): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val id = if (review.id.isEmpty()) UUID.randomUUID().toString() else review.id
        val finalReview = review.copy(id = id)
        
        reviewsRef.child(review.destinationId).child(id).setValue(finalReview)
            .addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(NetworkResult.Error(e.message ?: "Lỗi lưu đánh giá"))
                close()
            }
        awaitClose()
    }

    override fun deleteReview(destinationId: String, reviewId: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        reviewsRef.child(destinationId).child(reviewId).removeValue()
            .addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(NetworkResult.Error(e.message ?: "Lỗi xóa đánh giá"))
                close()
            }
        awaitClose()
    }
}
