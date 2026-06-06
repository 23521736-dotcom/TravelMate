package com.example.travelmate.data.repository

import com.example.travelmate.domain.model.Notification
import com.example.travelmate.domain.repository.NotificationRepository
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

class NotificationRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase
) : NotificationRepository {

    private val notifRef = database.getReference("notifications")

    override fun getUserNotifications(userId: String): Flow<NetworkResult<List<Notification>>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val listener = notifRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val list = mutableListOf<Notification>()
                    snapshot.children.forEach { child ->
                        try {
                            child.getValue(Notification::class.java)?.let { list.add(it) }
                        } catch (e: Exception) {
                            android.util.Log.e("NotifRepo", "Error mapping: ${e.message}")
                        }
                    }
                    trySend(NetworkResult.Success(list.sortedByDescending { it.timestamp }))
                } catch (e: Exception) {
                    trySend(NetworkResult.Error("Lỗi đồng bộ thông báo"))
                }
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(NetworkResult.Error(error.message))
            }
        })
        awaitClose { notifRef.child(userId).removeEventListener(listener) }
    }

    override fun markAsRead(userId: String, notificationId: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        notifRef.child(userId).child(notificationId).child("isRead").setValue(true)
            .addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }
        awaitClose()
    }

    override fun markAllAsRead(userId: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        notifRef.child(userId).get().addOnSuccessListener { snapshot ->
            val updates = mutableMapOf<String, Any>()
            snapshot.children.forEach { child ->
                updates["${child.key}/isRead"] = true
            }
            notifRef.child(userId).updateChildren(updates).addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }
        }
        awaitClose()
    }

    override fun addNotification(notification: Notification): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val id = if (notification.id.isEmpty()) UUID.randomUUID().toString() else notification.id
        notifRef.child(notification.userId).child(id).setValue(notification.copy(id = id))
            .addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }
        awaitClose()
    }
}
