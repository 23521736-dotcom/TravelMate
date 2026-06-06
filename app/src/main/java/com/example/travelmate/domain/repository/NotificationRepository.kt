package com.example.travelmate.domain.repository

import com.example.travelmate.domain.model.Notification
import com.example.travelmate.util.NetworkResult
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getUserNotifications(userId: String): Flow<NetworkResult<List<Notification>>>
    fun markAsRead(userId: String, notificationId: String): Flow<NetworkResult<Unit>>
    fun markAllAsRead(userId: String): Flow<NetworkResult<Unit>>
    fun addNotification(notification: Notification): Flow<NetworkResult<Unit>>
}
