package com.example.travelmate.data.repository

import com.example.travelmate.domain.model.Review
import com.example.travelmate.domain.repository.ReviewRepository
import com.example.travelmate.util.NetworkResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class ReviewRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewRepository {

    private val reviewsRef = firestore.collection("reviews")
    private val userReviewsRef = firestore.collection("user_reviews")

    private fun destinationReviews(destinationId: String) =
        reviewsRef.document(destinationId).collection("items")

    private fun userReviews(userId: String) =
        userReviewsRef.document(userId).collection("items")

    override fun getReviewsForDestination(destinationId: String): Flow<NetworkResult<List<Review>>> = callbackFlow {
        android.util.Log.d("ReviewRepo", "Fetching reviews for destination: $destinationId")
        trySend(NetworkResult.Loading)

        val registration = destinationReviews(destinationId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ReviewRepo", "Firestore error: ${error.message}")
                    trySend(NetworkResult.Error("Loi ket noi Firebase"))
                    return@addSnapshotListener
                }

                try {
                    val reviews = snapshot?.documents
                        ?.mapNotNull { doc -> doc.toObject(Review::class.java)?.copy(id = doc.id, destinationId = destinationId) }
                        ?: emptyList()
                    android.util.Log.d("ReviewRepo", "Successfully fetched ${reviews.size} reviews for $destinationId")
                    trySend(NetworkResult.Success(reviews))
                } catch (e: Exception) {
                    android.util.Log.e("ReviewRepo", "Error processing reviews: ${e.message}")
                    trySend(NetworkResult.Error("Loi xu ly du lieu danh gia"))
                }
            }

        awaitClose { registration.remove() }
    }

    override fun getUserReviews(userId: String): Flow<NetworkResult<List<Review>>> = callbackFlow {
        android.util.Log.d("ReviewRepo", "Fetching reviews for user: $userId")
        trySend(NetworkResult.Loading)

        val registration = userReviews(userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(NetworkResult.Error("Loi ket noi Firebase"))
                    return@addSnapshotListener
                }

                try {
                    val reviews = snapshot?.documents
                        ?.mapNotNull { doc -> doc.toObject(Review::class.java)?.copy(id = doc.id, userId = userId) }
                        ?: emptyList()
                    trySend(NetworkResult.Success(reviews))
                } catch (e: Exception) {
                    trySend(NetworkResult.Error("Loi xu ly du lieu danh gia cua toi"))
                }
            }

        awaitClose { registration.remove() }
    }

    override fun addReview(review: Review): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val id = review.id.ifEmpty { UUID.randomUUID().toString() }
        val finalReview = review.copy(id = id)

        val batch = firestore.batch()
        batch.set(destinationReviews(review.destinationId).document(id), finalReview)
        batch.set(userReviews(review.userId).document(id), finalReview)
        batch.commit()
            .addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(NetworkResult.Error(e.message ?: "Loi luu danh gia"))
                close()
            }
        awaitClose()
    }

    override fun deleteReview(destinationId: String, reviewId: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)

        destinationReviews(destinationId).document(reviewId).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    trySend(NetworkResult.Error("Khong tim thay danh gia de xoa"))
                    close()
                    return@addOnSuccessListener
                }

                val userId = snapshot.getString("userId") ?: ""
                val batch = firestore.batch()
                batch.delete(destinationReviews(destinationId).document(reviewId))
                if (userId.isNotEmpty()) {
                    batch.delete(userReviews(userId).document(reviewId))
                }
                batch.commit()
                    .addOnSuccessListener {
                        trySend(NetworkResult.Success(Unit))
                        close()
                    }
                    .addOnFailureListener { e ->
                        trySend(NetworkResult.Error(e.message ?: "Loi xoa danh gia"))
                        close()
                    }
            }
            .addOnFailureListener { e ->
                trySend(NetworkResult.Error(e.message ?: "Loi khi lay thong tin danh gia"))
                close()
            }

        awaitClose()
    }

    override suspend fun updateUserInReviews(userId: String, newName: String, newPhotoUrl: String?) {
        try {
            val snapshot = userReviews(userId).get().await()
            if (!snapshot.isEmpty) {
                val batch = firestore.batch()
                snapshot.documents.forEach { doc ->
                    val destinationId = doc.getString("destinationId")
                    val updates = mapOf("userName" to newName, "userPhotoUrl" to newPhotoUrl)
                    batch.update(doc.reference, updates)
                    if (!destinationId.isNullOrEmpty()) {
                        batch.update(destinationReviews(destinationId).document(doc.id), updates)
                    }
                }
                batch.commit().await()
                android.util.Log.d("ReviewRepo", "Successfully updated user info in ${snapshot.size()} review locations")
            }
        } catch (e: Exception) {
            android.util.Log.e("ReviewRepo", "Error updating user info in reviews: ${e.message}")
        }
    }
}
