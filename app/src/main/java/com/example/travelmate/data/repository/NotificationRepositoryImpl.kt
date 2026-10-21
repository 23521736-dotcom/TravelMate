package com.example.travelmate.data.repository

import com.example.travelmate.domain.model.Notification
import com.example.travelmate.domain.repository.NotificationRepository
import com.example.travelmate.util.NetworkResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    private val notificationsRef = firestore.collection("notifications")

    private fun userNotifications(userId: String) =
        notificationsRef.document(userId).collection("items")

    override fun getUserNotifications(userId: String): Flow<NetworkResult<List<Notification>>> = callbackFlow {
        android.util.Log.d("NotifRepo", "Fetching notifications for user: $userId")
        trySend(NetworkResult.Loading)

        val registration = userNotifications(userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("NotifRepo", "Firestore error: ${error.message}")
                    trySend(NetworkResult.Error(error.message ?: "Loi dong bo thong bao"))
                    return@addSnapshotListener
                }

                try {
                    val list = snapshot?.documents
                        ?.mapNotNull { doc -> doc.toObject(Notification::class.java)?.copy(id = doc.id) }
                        ?: emptyList()
                    android.util.Log.d("NotifRepo", "Successfully fetched ${list.size} notifications")
                    trySend(NetworkResult.Success(list))
                } catch (e: Exception) {
                    android.util.Log.e("NotifRepo", "Error syncing notifications", e)
                    trySend(NetworkResult.Error("Loi dong bo thong bao"))
                }
            }

        awaitClose { registration.remove() }
    }

    override fun markAsRead(userId: String, notificationId: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        userNotifications(userId).document(notificationId).update("isRead", true)
            .addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(NetworkResult.Error(e.message ?: "Failed to update notification"))
                close()
            }
        awaitClose()
    }

    override fun markAllAsRead(userId: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        userNotifications(userId).get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit()
                    .addOnSuccessListener {
                        trySend(NetworkResult.Success(Unit))
                        close()
                    }
                    .addOnFailureListener { e ->
                        trySend(NetworkResult.Error(e.message ?: "Failed to update notifications"))
                        close()
                    }
            }
            .addOnFailureListener { e ->
                trySend(NetworkResult.Error(e.message ?: "Failed to fetch notifications"))
                close()
            }
        awaitClose()
    }

    override fun addNotification(notification: Notification): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val id = notification.id.ifEmpty { UUID.randomUUID().toString() }
        userNotifications(notification.userId).document(id).set(notification.copy(id = id))
            .addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(NetworkResult.Error(e.message ?: "Failed to add notification"))
                close()
            }
        awaitClose()
    }
}
