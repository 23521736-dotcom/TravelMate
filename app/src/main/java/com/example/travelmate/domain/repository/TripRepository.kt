package com.example.travelmate.domain.repository

import com.example.travelmate.domain.model.Expense
import com.example.travelmate.domain.model.Trip
import com.example.travelmate.domain.model.TripActivity
import com.example.travelmate.domain.model.TripMember
import com.example.travelmate.util.NetworkResult
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    fun createTrip(trip: Trip): Flow<NetworkResult<String>>
    fun getUserTrips(userId: String): Flow<NetworkResult<List<Trip>>>
    fun getTripById(tripId: String): Flow<NetworkResult<Trip>>
    fun updateTrip(trip: Trip): Flow<NetworkResult<Unit>>
    fun deleteTrip(tripId: String): Flow<NetworkResult<Unit>>
    fun addActivity(tripId: String, dayIndex: Int, activity: TripActivity): Flow<NetworkResult<Unit>>
    fun updateActivity(tripId: String, dayIndex: Int, activity: TripActivity): Flow<NetworkResult<Unit>>
    fun toggleActivityDone(tripId: String, dayIndex: Int, activityId: String): Flow<NetworkResult<Unit>>
    fun deleteActivity(tripId: String, dayIndex: Int, activityId: String): Flow<NetworkResult<Unit>>
    fun addExpense(expense: Expense): Flow<NetworkResult<Unit>>
    fun updateExpense(expense: Expense): Flow<NetworkResult<Unit>>
    fun deleteExpense(expenseId: String): Flow<NetworkResult<Unit>>
    fun getTripExpenses(tripId: String): Flow<NetworkResult<List<Expense>>>
    fun getAllExpenses(userId: String): Flow<NetworkResult<List<Expense>>>
    fun addGalleryPhoto(tripId: String, photoUrl: String): Flow<NetworkResult<Unit>>
    fun deleteGalleryPhoto(tripId: String, photoUrl: String): Flow<NetworkResult<Unit>>
    fun uploadImage(path: String, byteArray: ByteArray): Flow<NetworkResult<String>>

    // Group Management
    fun getTripMembers(tripId: String): Flow<NetworkResult<List<TripMember>>>
    fun updateMemberRole(tripId: String, userId: String, role: String): Flow<NetworkResult<Unit>>
    fun transferOwnership(tripId: String, currentOwnerId: String, newOwnerId: String): Flow<NetworkResult<Unit>>
    fun removeMember(tripId: String, userId: String): Flow<NetworkResult<Unit>>
    fun joinTripByInviteCode(inviteCode: String, userId: String, userName: String, userPhotoUrl: String?): Flow<NetworkResult<String>>
    fun joinTripByShareToken(shareToken: String, userId: String, userName: String, userPhotoUrl: String?): Flow<NetworkResult<String>>
    fun leaveTrip(tripId: String, userId: String): Flow<NetworkResult<Unit>>

    // Audit Log
    fun getTripLogs(tripId: String): Flow<NetworkResult<List<com.example.travelmate.domain.model.TripLog>>>
    fun addTripLog(log: com.example.travelmate.domain.model.TripLog): Flow<NetworkResult<Unit>>
    suspend fun updateUserInTripMembers(userId: String, newName: String, newPhotoUrl: String?)
}
