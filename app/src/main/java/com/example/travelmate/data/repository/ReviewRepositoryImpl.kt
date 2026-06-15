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
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class ReviewRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase
) : ReviewRepository {

    private val reviewsRef = database.getReference("reviews")
    private val userReviewsRef = database.getReference("user_reviews")

    override fun getReviewsForDestination(destinationId: String): Flow<NetworkResult<List<Review>>> = callbackFlow {
        android.util.Log.d("ReviewRepo", "Fetching reviews for destination: $destinationId")
        trySend(NetworkResult.Loading)
        
        val listener = reviewsRef.child(destinationId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val reviews = mutableListOf<Review>()
                    if (snapshot.exists()) {
                        snapshot.children.forEach { child ->
                            try {
                                // Safe mapping for Review
                                val review = Review(
                                    id = child.child("id").getValue(String::class.java) ?: child.key ?: "",
                                    destinationId = child.child("destinationId").getValue(String::class.java) ?: destinationId,
                                    userId = child.child("userId").getValue(String::class.java) ?: "",
                                    userName = child.child("userName").getValue(String::class.java) ?: "Anonymous",
                                    userPhotoUrl = child.child("userPhotoUrl").getValue(String::class.java),
                                    rating = child.child("rating").getValue(Float::class.java) ?: child.child("rating").getValue(Double::class.java)?.toFloat() ?: 0f,
                                    comment = child.child("comment").getValue(String::class.java) ?: "",
                                    imageUrls = child.child("imageUrls").children.mapNotNull { it.getValue(String::class.java) },
                                    destinationName = child.child("destinationName").getValue(String::class.java) ?: "",
                                    destinationImage = child.child("destinationImage").getValue(String::class.java) ?: "",
                                    createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L
                                )
                                reviews.add(review)
                            } catch (e: Exception) {
                                android.util.Log.e("ReviewRepo", "Error parsing review ${child.key}: ${e.message}")
                            }
                        }
                    }
                    android.util.Log.d("ReviewRepo", "Successfully fetched ${reviews.size} reviews for $destinationId")
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

    override fun getUserReviews(userId: String): Flow<NetworkResult<List<Review>>> = callbackFlow {
        android.util.Log.d("ReviewRepo", "Fetching reviews for user: $userId")
        trySend(NetworkResult.Loading)
        
        val listener = userReviewsRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val reviews = mutableListOf<Review>()
                    if (snapshot.exists()) {
                        snapshot.children.forEach { child ->
                            try {
                                val review = Review(
                                    id = child.child("id").getValue(String::class.java) ?: child.key ?: "",
                                    destinationId = child.child("destinationId").getValue(String::class.java) ?: "",
                                    userId = userId,
                                    userName = child.child("userName").getValue(String::class.java) ?: "Anonymous",
                                    userPhotoUrl = child.child("userPhotoUrl").getValue(String::class.java),
                                    rating = child.child("rating").getValue(Float::class.java) ?: child.child("rating").getValue(Double::class.java)?.toFloat() ?: 0f,
                                    comment = child.child("comment").getValue(String::class.java) ?: "",
                                    imageUrls = child.child("imageUrls").children.mapNotNull { it.getValue(String::class.java) },
                                    destinationName = child.child("destinationName").getValue(String::class.java) ?: "",
                                    destinationImage = child.child("destinationImage").getValue(String::class.java) ?: "",
                                    createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L
                                )
                                reviews.add(review)
                            } catch (e: Exception) {
                                android.util.Log.e("ReviewRepo", "Error parsing user review ${child.key}: ${e.message}")
                            }
                        }
                    }
                    trySend(NetworkResult.Success(reviews.sortedByDescending { it.createdAt }))
                } catch (e: Exception) {
                    trySend(NetworkResult.Error("Lỗi xử lý dữ liệu đánh giá của tôi"))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(NetworkResult.Error("Lỗi kết nối Firebase"))
            }
        })
        awaitClose { userReviewsRef.child(userId).removeEventListener(listener) }
    }

    override fun addReview(review: Review): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val id = if (review.id.isEmpty()) UUID.randomUUID().toString() else review.id
        val finalReview = review.copy(id = id)
        
        val updates = hashMapOf<String, Any>(
            "reviews/${review.destinationId}/$id" to finalReview,
            "user_reviews/${review.userId}/$id" to finalReview
        )
        
        database.reference.updateChildren(updates)
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
        
        // Cần userId để xóa trong user_reviews, nhưng repository không nhận userId.
        // Giải pháp: Xóa trong reviews trước, sau đó nếu thành công thì xóa trong user_reviews (nếu có thông tin).
        // Tuy nhiên, addReview lưu cả hai nơi, vậy nên xóa cả hai.
        // Ta sẽ lấy review trước để lấy userId.
        
        reviewsRef.child(destinationId).child(reviewId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val userId = snapshot.child("userId").getValue(String::class.java) ?: ""
                
                val updates = hashMapOf<String, Any?>(
                    "reviews/$destinationId/$reviewId" to null,
                    "user_reviews/$userId/$reviewId" to null
                )
                
                database.reference.updateChildren(updates)
                    .addOnSuccessListener {
                        trySend(NetworkResult.Success(Unit))
                        close()
                    }
                    .addOnFailureListener { e ->
                        trySend(NetworkResult.Error(e.message ?: "Lỗi xóa đánh giá"))
                        close()
                    }
            } else {
                trySend(NetworkResult.Error("Không tìm thấy đánh giá để xóa"))
                close()
            }
        }.addOnFailureListener { e ->
            trySend(NetworkResult.Error(e.message ?: "Lỗi khi lấy thông tin đánh giá"))
            close()
        }

        awaitClose()
    }

    override suspend fun updateUserInReviews(userId: String, newName: String, newPhotoUrl: String?) {
        try {
            val snapshot = userReviewsRef.child(userId).get().await()
            if (snapshot.exists()) {
                val updates = hashMapOf<String, Any?>()
                snapshot.children.forEach { childSnapshot ->
                    val reviewId = childSnapshot.key
                    val destinationId = childSnapshot.child("destinationId").getValue(String::class.java)
                    if (reviewId != null) {
                        // Cập nhật trong user_reviews
                        updates["user_reviews/$userId/$reviewId/userName"] = newName
                        updates["user_reviews/$userId/$reviewId/userPhotoUrl"] = newPhotoUrl
                        
                        // Cập nhật trong reviews theo địa danh
                        if (destinationId != null) {
                            updates["reviews/$destinationId/$reviewId/userName"] = newName
                            updates["reviews/$destinationId/$reviewId/userPhotoUrl"] = newPhotoUrl
                        }
                    }
                }
                if (updates.isNotEmpty()) {
                    database.reference.updateChildren(updates).await()
                    android.util.Log.d("ReviewRepo", "Successfully updated user info in ${updates.size / 2} review locations")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ReviewRepo", "Error updating user info in reviews: ${e.message}")
        }
    }
}
