package com.example.travelmate.data.repository

import com.example.travelmate.data.remote.SupabaseService
import com.example.travelmate.domain.model.Expense
import com.example.travelmate.domain.model.ItineraryDay
import com.example.travelmate.domain.model.Trip
import com.example.travelmate.domain.model.TripActivity
import com.example.travelmate.domain.model.TripLog
import com.example.travelmate.domain.model.TripMember
import com.example.travelmate.domain.repository.TripRepository
import com.example.travelmate.util.NetworkResult
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class TripRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val supabaseService: SupabaseService
) : TripRepository {

    private val tripsRef = firestore.collection("trips")
    private val expensesRef = firestore.collection("expenses")
    private val tripMembersRef = firestore.collection("tripMembers")
    private val userTripsRef = firestore.collection("userTrips")
    private val inviteCodesRef = firestore.collection("inviteCodes")
    private val shareTokensRef = firestore.collection("shareTokens")
    private val usersRef = firestore.collection("users")
    private val tripLogsRef = firestore.collection("tripLogs")

    private fun tripMembers(tripId: String) =
        tripMembersRef.document(tripId).collection("members")

    private fun userTrips(userId: String) =
        userTripsRef.document(userId).collection("trips")

    private fun tripLogs(tripId: String) =
        tripLogsRef.document(tripId).collection("logs")

    override fun createTrip(trip: Trip): Flow<NetworkResult<String>> = callbackFlow {
        android.util.Log.d("TripRepo", "Creating trip for destination: ${trip.destination}")
        trySend(NetworkResult.Loading)

        val tripId = trip.id.ifEmpty { tripsRef.document().id }
        val shareToken = "t_" + UUID.randomUUID().toString().replace("-", "").take(12)
        val inviteCode = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
        val finalTrip = trip.copy(id = tripId, shareToken = shareToken, inviteCode = inviteCode)

        launch {
            try {
                val userSnapshot = usersRef.document(trip.userId).get().await()
                val userName = userSnapshot.getString("fullName")
                    ?: userSnapshot.getString("displayName")
                    ?: "Traveler"
                val userPhotoUrl = userSnapshot.getString("photoUrl")

                val member = TripMember(
                    userId = trip.userId,
                    tripId = tripId,
                    role = TripMember.ROLE_OWNER,
                    status = TripMember.STATUS_JOINED,
                    userName = userName,
                    userPhotoUrl = userPhotoUrl
                )

                val batch = firestore.batch()
                batch.set(tripsRef.document(tripId), finalTrip)
                batch.set(tripMembers(tripId).document(trip.userId), member)
                batch.set(userTrips(trip.userId).document(tripId), mapOf("tripId" to tripId, "joinedAt" to System.currentTimeMillis()))
                batch.set(inviteCodesRef.document(inviteCode), mapOf("tripId" to tripId))
                batch.set(shareTokensRef.document(shareToken), mapOf("tripId" to tripId))
                batch.commit().await()

                android.util.Log.d("TripRepo", "Trip created successfully with ID: $tripId")
                trySend(NetworkResult.Success(tripId))
            } catch (e: Exception) {
                android.util.Log.e("TripRepo", "Failed to create trip", e)
                trySend(NetworkResult.Error("Loi tao chuyen di: ${e.message}"))
            } finally {
                close()
            }
        }
        awaitClose()
    }

    override fun getUserTrips(userId: String): Flow<NetworkResult<List<Trip>>> = callbackFlow {
        android.util.Log.d("TripRepo", "Initiating getUserTrips listener for $userId")
        trySend(NetworkResult.Loading)

        var migrationChecked = false
        val registration = userTrips(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("TripRepo", "Firestore listener cancelled: ${error.message}")
                trySend(NetworkResult.Error("Ket noi Firebase bi gian doan"))
                return@addSnapshotListener
            }

            val tripIds = snapshot?.documents?.map { it.id }.orEmpty()
            launch {
                try {
                    if (tripIds.isEmpty() && !migrationChecked) {
                        migrationChecked = true
                        val legacyTrips = tripsRef.whereEqualTo("userId", userId).get().await().documents
                        if (legacyTrips.isNotEmpty()) {
                            val userSnapshot = usersRef.document(userId).get().await()
                            val userName = userSnapshot.getString("fullName")
                                ?: userSnapshot.getString("displayName")
                                ?: "Traveler"
                            val userPhotoUrl = userSnapshot.getString("photoUrl")
                            val batch = firestore.batch()
                            legacyTrips.forEach { doc ->
                                val tripId = doc.id
                                val member = TripMember(
                                    userId = userId,
                                    tripId = tripId,
                                    role = TripMember.ROLE_OWNER,
                                    status = TripMember.STATUS_JOINED,
                                    userName = userName,
                                    userPhotoUrl = userPhotoUrl
                                )
                                batch.set(userTrips(userId).document(tripId), mapOf("tripId" to tripId), SetOptions.merge())
                                batch.set(tripMembers(tripId).document(userId), member, SetOptions.merge())
                            }
                            batch.commit().await()
                            return@launch
                        }
                    }

                    val trips = coroutineScope {
                        tripIds.map { id ->
                            async {
                                try {
                                    tripsRef.document(id).get().await().toObject(Trip::class.java)
                                        ?.copy(id = id)
                                        ?.let { it.copy(status = it.getCalculatedStatus()) }
                                } catch (e: Exception) {
                                    android.util.Log.e("TripRepo", "Individual fetch error for $id", e)
                                    null
                                }
                            }
                        }.awaitAll().filterNotNull()
                    }
                    trySend(NetworkResult.Success(trips.distinctBy { it.id }.sortedByDescending { it.createdAt }))
                } catch (e: Exception) {
                    android.util.Log.e("TripRepo", "Sync flow crashed", e)
                    trySend(NetworkResult.Error("Loi dong bo: ${e.message}"))
                }
            }
        }

        awaitClose { registration.remove() }
    }

    override fun getTripById(tripId: String): Flow<NetworkResult<Trip>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val registration = tripsRef.document(tripId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(NetworkResult.Error("Loi ket noi: ${error.message}"))
                return@addSnapshotListener
            }

            try {
                val trip = snapshot?.toObject(Trip::class.java)?.copy(id = snapshot.id)
                if (trip != null) {
                    trySend(NetworkResult.Success(trip.copy(status = trip.getCalculatedStatus())))
                } else {
                    trySend(NetworkResult.Error("Khong tim thay chuyen di"))
                }
            } catch (e: Exception) {
                android.util.Log.e("TripRepo", "Error mapping trip $tripId", e)
                trySend(NetworkResult.Error("Loi du lieu chuyen di: ${e.message}"))
            }
        }
        awaitClose { registration.remove() }
    }

    override fun updateTrip(trip: Trip): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        tripsRef.document(trip.id).set(trip)
            .addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(NetworkResult.Error("Cap nhat that bai: ${e.message}"))
                close()
            }
        awaitClose()
    }

    override fun deleteTrip(tripId: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        launch {
            try {
                val trip = tripsRef.document(tripId).get().await().toObject(Trip::class.java)
                val members = tripMembers(tripId).get().await().documents
                val expenses = expensesRef.whereEqualTo("tripId", tripId).get().await().documents
                val logs = tripLogs(tripId).get().await().documents

                val batch = firestore.batch()
                if (trip != null) {
                    if (trip.inviteCode.isNotEmpty()) batch.delete(inviteCodesRef.document(trip.inviteCode))
                    if (trip.shareToken.isNotEmpty()) batch.delete(shareTokensRef.document(trip.shareToken))
                }
                members.forEach { member ->
                    batch.delete(userTrips(member.id).document(tripId))
                    batch.delete(member.reference)
                }
                expenses.forEach { batch.delete(it.reference) }
                logs.forEach { batch.delete(it.reference) }
                batch.delete(tripsRef.document(tripId))
                batch.commit().await()

                trySend(NetworkResult.Success(Unit))
            } catch (e: Exception) {
                trySend(NetworkResult.Error("Xoa that bai: ${e.message}"))
            } finally {
                close()
            }
        }
        awaitClose()
    }

    override fun addActivity(tripId: String, dayIndex: Int, activity: TripActivity): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        launch {
            try {
                val tripRef = tripsRef.document(tripId)
                val trip = tripRef.get().await().toObject(Trip::class.java)
                if (trip != null) {
                    val updatedItinerary = trip.itinerary.toMutableList()
                    while (updatedItinerary.size <= dayIndex) {
                        updatedItinerary.add(ItineraryDay(dayNumber = updatedItinerary.size + 1))
                    }
                    val day = updatedItinerary[dayIndex]
                    val activityId = activity.id.ifEmpty { tripsRef.document().id }
                    val sortedActivities = (day.activities + activity.copy(id = activityId)).sortedBy { parseTimeHelper(it.time) }
                    updatedItinerary[dayIndex] = day.copy(activities = sortedActivities)
                    tripRef.update("itinerary", updatedItinerary).await()
                    trySend(NetworkResult.Success(Unit))
                } else {
                    trySend(NetworkResult.Error("Khong tim thay chuyen di"))
                }
            } catch (e: Exception) {
                trySend(NetworkResult.Error("Loi them hoat dong: ${e.message}"))
            } finally {
                close()
            }
        }
        awaitClose()
    }

    override fun updateActivity(tripId: String, dayIndex: Int, activity: TripActivity): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        launch {
            try {
                val tripRef = tripsRef.document(tripId)
                val trip = tripRef.get().await().toObject(Trip::class.java)
                if (trip != null && dayIndex < trip.itinerary.size) {
                    val updatedItinerary = trip.itinerary.toMutableList()
                    val day = updatedItinerary[dayIndex]
                    val sortedActivities = day.activities
                        .map { if (it.id == activity.id) activity else it }
                        .sortedBy { parseTimeHelper(it.time) }
                    updatedItinerary[dayIndex] = day.copy(activities = sortedActivities)
                    tripRef.update("itinerary", updatedItinerary).await()
                    trySend(NetworkResult.Success(Unit))
                } else {
                    trySend(NetworkResult.Error("Khong tim thay du lieu"))
                }
            } catch (e: Exception) {
                trySend(NetworkResult.Error("Loi cap nhat: ${e.message}"))
            } finally {
                close()
            }
        }
        awaitClose()
    }

    private fun parseTimeHelper(timeStr: String): Int {
        return try {
            val parts = timeStr.uppercase().split(" ")
            val timeParts = parts[0].split(":")
            var hour = timeParts[0].toInt()
            val minute = if (timeParts.size > 1) timeParts[1].toInt() else 0
            val isPm = parts.size > 1 && (parts[1] == "CH" || parts[1] == "PM")
            if (isPm && hour < 12) hour += 12
            if (!isPm && hour == 12) hour = 0
            hour * 60 + minute
        } catch (e: Exception) {
            0
        }
    }

    override fun toggleActivityDone(tripId: String, dayIndex: Int, activityId: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        launch {
            try {
                val tripRef = tripsRef.document(tripId)
                val trip = tripRef.get().await().toObject(Trip::class.java)
                if (trip != null && dayIndex < trip.itinerary.size) {
                    val updatedItinerary = trip.itinerary.toMutableList()
                    val day = updatedItinerary[dayIndex]
                    updatedItinerary[dayIndex] = day.copy(
                        activities = day.activities.map { if (it.id == activityId) it.copy(done = !it.done) else it }
                    )
                    tripRef.update("itinerary", updatedItinerary).await()
                    trySend(NetworkResult.Success(Unit))
                } else {
                    trySend(NetworkResult.Error("Khong tim thay du lieu"))
                }
            } catch (e: Exception) {
                trySend(NetworkResult.Error("Loi cap nhat trang thai"))
            } finally {
                close()
            }
        }
        awaitClose()
    }

    override fun deleteActivity(tripId: String, dayIndex: Int, activityId: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        launch {
            try {
                val tripRef = tripsRef.document(tripId)
                val trip = tripRef.get().await().toObject(Trip::class.java)
                if (trip != null && dayIndex < trip.itinerary.size) {
                    val updatedItinerary = trip.itinerary.toMutableList()
                    val day = updatedItinerary[dayIndex]
                    updatedItinerary[dayIndex] = day.copy(activities = day.activities.filter { it.id != activityId })
                    tripRef.update("itinerary", updatedItinerary).await()
                    trySend(NetworkResult.Success(Unit))
                } else {
                    trySend(NetworkResult.Error("Khong tim thay du lieu"))
                }
            } catch (e: Exception) {
                trySend(NetworkResult.Error("Loi xoa hoat dong"))
            } finally {
                close()
            }
        }
        awaitClose()
    }

    override fun addExpense(expense: Expense): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val expenseId = expense.id.ifEmpty { expensesRef.document().id }
        val finalExpense = expense.copy(id = expenseId)
        expensesRef.document(expenseId).set(finalExpense)
            .addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(NetworkResult.Error("Loi luu chi tieu: ${e.message}"))
                close()
            }
        awaitClose()
    }

    override fun updateExpense(expense: Expense): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        expensesRef.document(expense.id).set(expense)
            .addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(NetworkResult.Error("Loi cap nhat chi tieu: ${e.message}"))
                close()
            }
        awaitClose()
    }

    override fun deleteExpense(expenseId: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        expensesRef.document(expenseId).delete()
            .addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }
            .addOnFailureListener {
                trySend(NetworkResult.Error("Loi xoa chi tieu"))
                close()
            }
        awaitClose()
    }

    override fun getTripExpenses(tripId: String): Flow<NetworkResult<List<Expense>>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val registration = expensesRef.whereEqualTo("tripId", tripId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(NetworkResult.Error("Loi tai chi tieu: ${error.message}"))
                return@addSnapshotListener
            }
            try {
                val list = snapshot?.documents?.mapNotNull { it.toObject(Expense::class.java)?.copy(id = it.id) }.orEmpty()
                trySend(NetworkResult.Success(list))
            } catch (e: Exception) {
                android.util.Log.e("TripRepo", "Error mapping expenses for trip $tripId", e)
            }
        }
        awaitClose { registration.remove() }
    }

    override fun getAllExpenses(userId: String): Flow<NetworkResult<List<Expense>>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val registration = expensesRef.whereEqualTo("userId", userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(NetworkResult.Error(error.message ?: "Loi tai chi tieu"))
                return@addSnapshotListener
            }
            try {
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject(Expense::class.java)?.copy(id = it.id) }
                    ?.sortedByDescending { it.date }
                    .orEmpty()
                trySend(NetworkResult.Success(list))
            } catch (e: Exception) {
                trySend(NetworkResult.Error("Loi xu ly du lieu chi tieu"))
            }
        }
        awaitClose { registration.remove() }
    }

    override fun addGalleryPhoto(tripId: String, photoUrl: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        tripsRef.document(tripId).update("galleryUrls", FieldValue.arrayUnion(photoUrl))
            .addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(NetworkResult.Error("Loi them anh: ${e.message}"))
                close()
            }
        awaitClose()
    }

    override fun deleteGalleryPhoto(tripId: String, photoUrl: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        tripsRef.document(tripId).update("galleryUrls", FieldValue.arrayRemove(photoUrl))
            .addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(NetworkResult.Error("Loi xoa anh: ${e.message}"))
                close()
            }
        awaitClose()
    }

    override fun uploadImage(path: String, byteArray: ByteArray): Flow<NetworkResult<String>> = callbackFlow {
        trySend(NetworkResult.Loading)
        launch {
            try {
                val url = supabaseService.uploadImage(path, byteArray)
                trySend(NetworkResult.Success(url))
            } catch (e: Exception) {
                trySend(NetworkResult.Error(e.message ?: "Upload failed"))
            } finally {
                close()
            }
        }
        awaitClose()
    }

    override fun getTripMembers(tripId: String): Flow<NetworkResult<List<TripMember>>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val registration = tripMembers(tripId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(NetworkResult.Error("Loi tai thanh vien: ${error.message}"))
                return@addSnapshotListener
            }
            try {
                val members = snapshot?.documents?.mapNotNull { it.toObject(TripMember::class.java)?.copy(userId = it.id) }.orEmpty()
                trySend(NetworkResult.Success(members))
            } catch (e: Exception) {
                android.util.Log.e("TripRepo", "Error mapping members for trip $tripId", e)
            }
        }
        awaitClose { registration.remove() }
    }

    override fun updateMemberRole(tripId: String, userId: String, role: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        tripMembers(tripId).document(userId).update("role", role)
            .addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(NetworkResult.Error(e.message ?: "Failed to update role"))
                close()
            }
        awaitClose()
    }

    override fun transferOwnership(tripId: String, currentOwnerId: String, newOwnerId: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        launch {
            try {
                val batch = firestore.batch()
                batch.update(tripMembers(tripId).document(currentOwnerId), "role", TripMember.ROLE_EDITOR)
                batch.update(tripMembers(tripId).document(newOwnerId), "role", TripMember.ROLE_OWNER)
                batch.update(tripsRef.document(tripId), "userId", newOwnerId)
                batch.commit().await()
                trySend(NetworkResult.Success(Unit))
            } catch (e: Exception) {
                trySend(NetworkResult.Error("Loi chuyen quyen: ${e.message}"))
            } finally {
                close()
            }
        }
        awaitClose()
    }

    override fun removeMember(tripId: String, userId: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        launch {
            try {
                val batch = firestore.batch()
                batch.delete(tripMembers(tripId).document(userId))
                batch.delete(userTrips(userId).document(tripId))
                batch.commit().await()
                trySend(NetworkResult.Success(Unit))
            } catch (e: Exception) {
                trySend(NetworkResult.Error(e.message ?: "Failed to remove member"))
            } finally {
                close()
            }
        }
        awaitClose()
    }

    override fun joinTripByInviteCode(inviteCode: String, userId: String, userName: String, userPhotoUrl: String?): Flow<NetworkResult<String>> = callbackFlow {
        val sanitizedCode = inviteCode.trim().uppercase()
        android.util.Log.d("TripRepo", "Join attempt with code: $sanitizedCode")
        trySend(NetworkResult.Loading)

        launch {
            try {
                val inviteDoc = inviteCodesRef.document(sanitizedCode).get().await()
                var tripId = inviteDoc.getString("tripId")

                if (tripId == null) {
                    val querySnap = tripsRef.whereEqualTo("inviteCode", sanitizedCode).limit(1).get().await()
                    tripId = querySnap.documents.firstOrNull()?.id
                }

                if (tripId != null) {
                    if (!inviteDoc.exists()) {
                        inviteCodesRef.document(sanitizedCode).set(mapOf("tripId" to tripId)).await()
                    }
                    joinTripInternal(tripId, userId, userName, userPhotoUrl, this@callbackFlow)
                } else {
                    trySend(NetworkResult.Error("Ma moi khong ton tai hoac da het han"))
                    close()
                }
            } catch (e: Exception) {
                android.util.Log.e("TripRepo", "Error joining trip via code", e)
                trySend(NetworkResult.Error("Loi ket noi: ${e.message}"))
                close()
            }
        }
        awaitClose()
    }

    override fun joinTripByShareToken(shareToken: String, userId: String, userName: String, userPhotoUrl: String?): Flow<NetworkResult<String>> = callbackFlow {
        trySend(NetworkResult.Loading)
        launch {
            try {
                val tokenDoc = shareTokensRef.document(shareToken).get().await()
                val tripId = tokenDoc.getString("tripId")

                if (tripId != null) {
                    joinTripInternal(tripId, userId, userName, userPhotoUrl, this@callbackFlow)
                } else {
                    trySend(NetworkResult.Error("Link chia se khong hop le"))
                    close()
                }
            } catch (e: Exception) {
                trySend(NetworkResult.Error(e.message ?: "Error joining trip"))
                close()
            }
        }
        awaitClose()
    }

    private suspend fun joinTripInternal(
        tripId: String,
        userId: String,
        userName: String,
        userPhotoUrl: String?,
        channel: kotlinx.coroutines.channels.ProducerScope<NetworkResult<String>>
    ) {
        val memberRef = tripMembers(tripId).document(userId)
        val memberSnap = memberRef.get().await()
        if (memberSnap.exists()) {
            channel.trySend(NetworkResult.Success(tripId))
            channel.close()
            return
        }

        val member = TripMember(
            userId = userId,
            tripId = tripId,
            role = TripMember.ROLE_VIEWER,
            status = TripMember.STATUS_JOINED,
            userName = userName,
            userPhotoUrl = userPhotoUrl
        )

        val batch = firestore.batch()
        batch.set(memberRef, member)
        batch.set(userTrips(userId).document(tripId), mapOf("tripId" to tripId, "joinedAt" to System.currentTimeMillis()))
        batch.commit().await()

        channel.trySend(NetworkResult.Success(tripId))
        channel.close()
    }

    override fun leaveTrip(tripId: String, userId: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        launch {
            try {
                val member = tripMembers(tripId).document(userId).get().await().toObject(TripMember::class.java)

                if (member?.role == TripMember.ROLE_OWNER) {
                    val allMembers = tripMembers(tripId).get().await()
                    if (allMembers.size() > 1) {
                        trySend(NetworkResult.Error("Vui long chuyen quyen chu so huu truoc khi roi nhom"))
                        close()
                        return@launch
                    }

                    deleteTrip(tripId).collect { result ->
                        when (result) {
                            is NetworkResult.Success -> {
                                trySend(NetworkResult.Success(Unit))
                                close()
                            }
                            is NetworkResult.Error -> {
                                trySend(NetworkResult.Error(result.message))
                                close()
                            }
                            NetworkResult.Loading -> Unit
                        }
                    }
                } else {
                    val batch = firestore.batch()
                    batch.delete(tripMembers(tripId).document(userId))
                    batch.delete(userTrips(userId).document(tripId))
                    batch.commit().await()
                    trySend(NetworkResult.Success(Unit))
                    close()
                }
            } catch (e: Exception) {
                trySend(NetworkResult.Error(e.message ?: "Error leaving trip"))
                close()
            }
        }
        awaitClose()
    }

    override fun getTripLogs(tripId: String): Flow<NetworkResult<List<TripLog>>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val registration = tripLogs(tripId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(NetworkResult.Error(error.message ?: "Failed to fetch logs"))
                    return@addSnapshotListener
                }
                try {
                    val logs = snapshot?.documents?.mapNotNull { it.toObject(TripLog::class.java)?.copy(id = it.id) }.orEmpty()
                    trySend(NetworkResult.Success(logs))
                } catch (e: Exception) {
                    android.util.Log.e("TripRepo", "Error mapping trip logs", e)
                }
            }
        awaitClose { registration.remove() }
    }

    override fun addTripLog(log: TripLog): Flow<NetworkResult<Unit>> = callbackFlow {
        val logId = log.id.ifEmpty { tripLogs(log.tripId).document().id }
        tripLogs(log.tripId).document(logId).set(log.copy(id = logId))
            .addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }
            .addOnFailureListener {
                trySend(NetworkResult.Error("Failed to add log"))
                close()
            }
        awaitClose()
    }

    override suspend fun updateUserInTripMembers(userId: String, newName: String, newPhotoUrl: String?) {
        try {
            val snapshot = userTrips(userId).get().await()
            if (!snapshot.isEmpty) {
                val batch = firestore.batch()
                snapshot.documents.forEach { doc ->
                    val updates = mapOf("userName" to newName, "userPhotoUrl" to newPhotoUrl)
                    batch.update(tripMembers(doc.id).document(userId), updates)
                }
                batch.commit().await()
                android.util.Log.d("TripRepo", "Successfully updated user info in ${snapshot.size()} trip member locations")
            }
        } catch (e: Exception) {
            android.util.Log.e("TripRepo", "Error updating user info in trip members: ${e.message}")
        }
    }
}
