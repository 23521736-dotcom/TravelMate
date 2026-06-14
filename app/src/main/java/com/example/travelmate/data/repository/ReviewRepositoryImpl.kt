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
        android.util.Log.d("ReviewRepo", "Fetching reviews for destination: $destinationId")
        trySend(NetworkResult.Loading)
        
        // Use single listener instead of real-time if preferred, or keep real-time
        val listener = reviewsRef.child(destinationId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val reviews = mutableListOf<Review>()
                    if (snapshot.exists()) {
                        snapshot.children.forEach { child ->
                            try {
                                child.getValue(Review::class.java)?.let { reviews.add(it) }
                            } catch (e: Exception) {
                                android.util.Log.e("ReviewRepo", "Error parsing review: ${e.message}")
                            }
                        }
                    }
                    android.util.Log.d("ReviewRepo", "Successfully fetched ${reviews.size} reviews")
                    trySend(NetworkResult.Success(reviews.sortedByDescending { it.createdAt }))
                } catch (e: Exception) {
                    android.util.Log.e("ReviewRepo", "Error processing reviews: ${e.message}")
                    trySend(NetworkResult.Error("Lỗi xử lý dữ liệu đánh giá"))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("ReviewRepo", "Firebase error: ${error.message}")
                trySend(NetworkResult.Error("Lỗi kết nối Firebase"))
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
