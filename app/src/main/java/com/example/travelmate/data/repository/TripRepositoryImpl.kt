package com.example.travelmate.data.repository

import com.example.travelmate.data.remote.SupabaseService
import com.example.travelmate.domain.model.Expense
import com.example.travelmate.domain.model.ItineraryDay
import com.example.travelmate.domain.model.Trip
import com.example.travelmate.domain.model.TripActivity
import com.example.travelmate.domain.model.TripMember
import com.example.travelmate.domain.repository.TripRepository
import com.example.travelmate.util.NetworkResult
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject

class TripRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val supabaseService: SupabaseService
) : TripRepository {

    private val tripsRef = database.getReference("trips")
    private val expensesRef = database.getReference("expenses")
    private val tripMembersRef = database.getReference("tripMembers")
    private val userTripsRef = database.getReference("userTrips")
    private val inviteCodesRef = database.getReference("inviteCodes")
    private val shareTokensRef = database.getReference("shareTokens")
    private val usersRef = database.getReference("users")
    private val tripLogsRef = database.getReference("tripLogs")

    override fun createTrip(trip: Trip): Flow<NetworkResult<String>> = callbackFlow {
        android.util.Log.d("TripRepo", "Creating trip for destination: ${trip.destination}")
        trySend(NetworkResult.Loading)
        
        val tripId = trip.id.ifEmpty { tripsRef.push().key ?: UUID.randomUUID().toString() }
        
        // Generate sharing codes
        val shareToken = "t_" + UUID.randomUUID().toString().replace("-", "").take(12)
        val inviteCode = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
        
        val finalTrip = trip.copy(
            id = tripId,
            shareToken = shareToken,
            inviteCode = inviteCode
        )
        
        launch {
            try {
                // 1. Fetch user info for cached member data
                val userSnapshot = usersRef.child(trip.userId).get().await()
                val userData = userSnapshot.value as? Map<*, *>
                val userName = userData?.get("fullName") as? String 
                    ?: userData?.get("displayName") as? String
                    ?: "Traveler"
                val userPhotoUrl = userData?.get("photoUrl") as? String
                
                val member = TripMember(
                    userId = trip.userId,
                    tripId = tripId,
                    role = TripMember.ROLE_OWNER,
                    status = TripMember.STATUS_JOINED,
                    userName = userName,
                    userPhotoUrl = userPhotoUrl
                )
                
                // 2. Perform atomic-like update using a map
                val updates = hashMapOf<String, Any>(
                    "trips/$tripId" to finalTrip,
                    "tripMembers/$tripId/${trip.userId}" to member,
                    "userTrips/${trip.userId}/$tripId" to true,
                    "inviteCodes/$inviteCode" to tripId,
                    "shareTokens/$shareToken" to tripId
                )
                
                database.reference.updateChildren(updates).await()
                
                android.util.Log.d("TripRepo", "Trip created successfully with ID: $tripId")
                trySend(NetworkResult.Success(tripId))
            } catch (e: Exception) {
                android.util.Log.e("TripRepo", "Failed to create trip", e)
                trySend(NetworkResult.Error("Lỗi tạo chuyến đi: ${e.message}"))
            } finally {
                close()
            }
        }
        awaitClose()
    }

    override fun getUserTrips(userId: String): Flow<NetworkResult<List<Trip>>> = callbackFlow {
        android.util.Log.d("TripRepo", "Initiating getUserTrips listener for $userId")
        trySend(NetworkResult.Loading)
        
        var isMigrationChecked = false

        val listener = userTripsRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tripIds = snapshot.children.mapNotNull { it.key }
                android.util.Log.d("TripRepo", "onDataChange: User $userId has ${tripIds.size} trip IDs")
                
                launch {
                    try {
                        if (tripIds.isEmpty() && !isMigrationChecked) {
                            isMigrationChecked = true
                            android.util.Log.d("TripRepo", "Checking legacy trips for user $userId")
                            
                            // Resilient migration check: Fallback to full fetch if indexing fails
                            val legacySnap = try {
                                tripsRef.orderByChild("userId").equalTo(userId).get().await()
                            } catch (e: Exception) {
                                android.util.Log.w("TripRepo", "Indexed query failed, falling back to manual filter", e)
                                tripsRef.get().await() // Fetch all and filter manually as fallback
                            }

                            if (legacySnap.exists()) {
                                // If we fetched everything, filter manually; otherwise use the indexed result
                                val tripsToMigrate = if (legacySnap.key == "trips") {
                                    legacySnap.children.filter { it.child("userId").value == userId }
                                } else {
                                    legacySnap.children.toList()
                                }

                                if (tripsToMigrate.isNotEmpty()) {
                                    android.util.Log.i("TripRepo", "Found ${tripsToMigrate.size} trips to migrate")
                                    performOneTimeMigrationResilient(userId, tripsToMigrate)
                                    return@launch
                                }
                            }
                        }

                        val tripsList = mutableListOf<Trip>()
                        if (tripIds.isNotEmpty()) {
                            coroutineScope {
                                val deferred = tripIds.map { id ->
                                    async {
                                        try {
                                            val tripSnap = tripsRef.child(id).get().await()
                                            val trip = tripSnap.getValue(Trip::class.java)
                                            trip?.copy(status = trip.getCalculatedStatus())
                                        } catch (e: Exception) {
                                            android.util.Log.e("TripRepo", "Individual fetch error for $id", e)
                                            null
                                        }
                                    }
                                }
                                tripsList.addAll(deferred.awaitAll().filterNotNull())
                            }
                        }

                        android.util.Log.d("TripRepo", "Final Success: Sending ${tripsList.size} trips to UI")
                        trySend(NetworkResult.Success(tripsList.distinctBy { it.id }.sortedByDescending { it.createdAt }))
                    } catch (e: Exception) {
                        android.util.Log.e("TripRepo", "Sync Flow Crashed", e)
                        trySend(NetworkResult.Error("Lỗi đồng bộ: ${e.message}"))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("TripRepo", "Firebase Listener Cancelled: ${error.message}")
                trySend(NetworkResult.Error("Kết nối Firebase bị gián đoạn"))
            }
        })
        
        awaitClose { 
            android.util.Log.d("TripRepo", "Removing getUserTrips listener for $userId")
            userTripsRef.child(userId).removeEventListener(listener) 
        }
    }

    private suspend fun performOneTimeMigrationResilient(userId: String, tripsToMigrate: List<DataSnapshot>) {
        try {
            android.util.Log.d("TripRepo", "Migrating legacy trips for user: $userId")
            val userSnapshot = usersRef.child(userId).get().await()
            val userData = userSnapshot.value as? Map<*, *>
            val userName = userData?.get("fullName") as? String 
                ?: userData?.get("displayName") as? String 
                ?: "Traveler"
            val userPhotoUrl = userData?.get("photoUrl") as? String
            
            val updates = hashMapOf<String, Any>()
            
            for (child in tripsToMigrate) {
                val trip = child.getValue(Trip::class.java)
                if (trip != null) {
                    val member = TripMember(
                        userId = userId,
                        tripId = trip.id,
                        role = TripMember.ROLE_OWNER,
                        status = TripMember.STATUS_JOINED,
                        userName = userName,
                        userPhotoUrl = userPhotoUrl
                    )
                    updates["userTrips/$userId/${trip.id}"] = true
                    updates["tripMembers/${trip.id}/$userId"] = member
                }
            }
            
            if (updates.isNotEmpty()) {
                android.util.Log.i("TripRepo", "Migrating ${updates.size / 2} trips to new structure")
                database.reference.updateChildren(updates).await()
            }
        } catch (e: Exception) {
            android.util.Log.e("TripRepo", "Migration failed", e)
        }
    }

    override fun getTripById(tripId: String): Flow<NetworkResult<Trip>> = callbackFlow {
        trySend(NetworkResult.Loading)
        
        val listener = tripsRef.child(tripId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val trip = snapshot.getValue(Trip::class.java)
                    if (trip != null) {
                        trySend(NetworkResult.Success(trip.copy(status = trip.getCalculatedStatus())))
                    } else {
                        trySend(NetworkResult.Error("Không tìm thấy chuyến đi"))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TripRepo", "Error mapping trip $tripId", e)
                    trySend(NetworkResult.Error("Lỗi dữ liệu chuyến đi: ${e.message}"))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(NetworkResult.Error("Lỗi kết nối: ${error.message}"))
            }
        })
        awaitClose { tripsRef.child(tripId).removeEventListener(listener) }
    }

    override fun updateTrip(trip: Trip): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        tripsRef.child(trip.id).setValue(trip).addOnSuccessListener {
            trySend(NetworkResult.Success(Unit))
            close()
        }.addOnFailureListener { e ->
            trySend(NetworkResult.Error("Cập nhật thất bại: ${e.message}"))
            close()
        }
        awaitClose()
    }

    override fun deleteTrip(tripId: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        
        launch {
            try {
                // Get trip to find codes and members
                val tripSnap = tripsRef.child(tripId).get().await()
                val trip = tripSnap.getValue(Trip::class.java)
                
                val updates = hashMapOf<String, Any?>()
                
                if (trip != null) {
                    // 1. Remove from inviteCodes and shareTokens
                    if (trip.inviteCode.isNotEmpty()) updates["inviteCodes/${trip.inviteCode}"] = null
                    if (trip.shareToken.isNotEmpty()) updates["shareTokens/${trip.shareToken}"] = null
                    
                    // 2. Remove from userTrips for all members
                    val membersSnap = tripMembersRef.child(tripId).get().await()
                    membersSnap.children.forEach { memberSnap ->
                        val memberUserId = memberSnap.key
                        if (memberUserId != null) {
                            updates["userTrips/$memberUserId/$tripId"] = null
                        }
                    }
                    
                    // 3. Remove members list
                    updates["tripMembers/$tripId"] = null
                }
                
                // 4. Finally delete the trip itself
                updates["trips/$tripId"] = null
                
                database.reference.updateChildren(updates).await()
                
                trySend(NetworkResult.Success(Unit))
            } catch (e: Exception) {
                trySend(NetworkResult.Error("Xóa thất bại: ${e.message}"))
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
                val snapshot = tripsRef.child(tripId).get().await()
                val trip = snapshot.getValue(Trip::class.java)
                if (trip != null) {
                    val updatedItinerary = trip.itinerary.toMutableList()
                    
                    while (updatedItinerary.size <= dayIndex) {
                        updatedItinerary.add(ItineraryDay(dayNumber = updatedItinerary.size + 1))
                    }

                    val day = updatedItinerary[dayIndex]
                    val updatedActivities = day.activities.toMutableList()
                    val activityId = tripsRef.push().key ?: UUID.randomUUID().toString()
                    updatedActivities.add(activity.copy(id = activityId))
                    
                    updatedItinerary[dayIndex] = day.copy(activities = updatedActivities)
                    
                    tripsRef.child(tripId).child("itinerary").setValue(updatedItinerary).await()
                    trySend(NetworkResult.Success(Unit))
                } else {
                    trySend(NetworkResult.Error("Không tìm thấy chuyến đi"))
                }
            } catch (e: Exception) {
                trySend(NetworkResult.Error("Lỗi thêm hoạt động: ${e.message}"))
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
                val snapshot = tripsRef.child(tripId).get().await()
                val trip = snapshot.getValue(Trip::class.java)
                if (trip != null && dayIndex < trip.itinerary.size) {
                    val updatedItinerary = trip.itinerary.toMutableList()
                    val day = updatedItinerary[dayIndex]
                    val updatedActivities = day.activities.map { 
                        if (it.id == activity.id) activity else it
                    }
                    updatedItinerary[dayIndex] = day.copy(activities = updatedActivities)
                    tripsRef.child(tripId).child("itinerary").setValue(updatedItinerary).await()
                    trySend(NetworkResult.Success(Unit))
                } else {
                    trySend(NetworkResult.Error("Không tìm thấy dữ liệu"))
                }
            } catch (e: Exception) {
                trySend(NetworkResult.Error("Lỗi cập nhật: ${e.message}"))
            } finally {
                close()
            }
        }
        awaitClose()
    }

    override fun toggleActivityDone(tripId: String, dayIndex: Int, activityId: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        launch {
            try {
                val snapshot = tripsRef.child(tripId).get().await()
                val trip = snapshot.getValue(Trip::class.java)
                if (trip != null && dayIndex < trip.itinerary.size) {
                    val updatedItinerary = trip.itinerary.toMutableList()
                    val day = updatedItinerary[dayIndex]
                    val updatedActivities = day.activities.map { 
                        if (it.id == activityId) it.copy(done = !it.done) else it
                    }
                    updatedItinerary[dayIndex] = day.copy(activities = updatedActivities)
                    tripsRef.child(tripId).child("itinerary").setValue(updatedItinerary).await()
                    trySend(NetworkResult.Success(Unit))
                }
            } catch (e: Exception) {
                trySend(NetworkResult.Error("Lỗi cập nhật trạng thái"))
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
                val snapshot = tripsRef.child(tripId).get().await()
                val trip = snapshot.getValue(Trip::class.java)
                if (trip != null && dayIndex < trip.itinerary.size) {
                    val updatedItinerary = trip.itinerary.toMutableList()
                    val day = updatedItinerary[dayIndex]
                    val updatedActivities = day.activities.filter { it.id != activityId }
                    updatedItinerary[dayIndex] = day.copy(activities = updatedActivities)
                    
                    tripsRef.child(tripId).child("itinerary").setValue(updatedItinerary).await()
                    trySend(NetworkResult.Success(Unit))
                }
            } catch (e: Exception) {
                trySend(NetworkResult.Error("Lỗi xóa hoạt động"))
            } finally {
                close()
            }
        }
        awaitClose()
    }

    override fun addExpense(expense: Expense): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val expenseId = if (expense.id.isEmpty()) expensesRef.push().key ?: UUID.randomUUID().toString() else expense.id
        val finalExpense = expense.copy(id = expenseId)
        
        expensesRef.child(expenseId).setValue(finalExpense).addOnSuccessListener {
            trySend(NetworkResult.Success(Unit))
            close()
        }.addOnFailureListener { e ->
            trySend(NetworkResult.Error("Lỗi lưu chi tiêu: ${e.message}"))
            close()
        }
        awaitClose()
    }

    override fun updateExpense(expense: Expense): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        expensesRef.child(expense.id).setValue(expense).addOnSuccessListener {
            trySend(NetworkResult.Success(Unit))
            close()
        }.addOnFailureListener { e ->
            trySend(NetworkResult.Error("Lỗi cập nhật chi tiêu: ${e.message}"))
            close()
        }
        awaitClose()
    }

    override fun deleteExpense(expenseId: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        expensesRef.child(expenseId).removeValue().addOnSuccessListener {
            trySend(NetworkResult.Success(Unit))
            close()
        }.addOnFailureListener { e ->
            trySend(NetworkResult.Error("Lỗi xóa chi tiêu"))
            close()
        }
        awaitClose()
    }

    override fun getTripExpenses(tripId: String): Flow<NetworkResult<List<Expense>>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val listener = expensesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val list = mutableListOf<Expense>()
                    snapshot.children.forEach { child ->
                        val expense = child.getValue(Expense::class.java)
                        if (expense != null && expense.tripId == tripId) {
                            list.add(expense)
                        }
                    }
                    trySend(NetworkResult.Success(list))
                } catch (e: Exception) {
                    android.util.Log.e("TripRepo", "Error mapping expenses for trip $tripId", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(NetworkResult.Error("Lỗi tải chi tiêu: ${error.message}"))
            }
        })
        awaitClose { expensesRef.removeEventListener(listener) }
    }

    override fun getAllExpenses(userId: String): Flow<NetworkResult<List<Expense>>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val listener = expensesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val list = mutableListOf<Expense>()
                    snapshot.children.forEach { child ->
                        try {
                            val expense = child.getValue(Expense::class.java)
                            if (expense != null && expense.userId == userId) {
                                list.add(expense)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("TripRepo", "Error mapping expense: ${e.message}")
                        }
                    }
                    trySend(NetworkResult.Success(list.sortedByDescending { it.date }))
                } catch (e: Exception) {
                    trySend(NetworkResult.Error("Lỗi xử lý dữ liệu chi tiêu"))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(NetworkResult.Error(error.message))
            }
        })
        awaitClose { expensesRef.removeEventListener(listener) }
    }

    override fun addGalleryPhoto(tripId: String, photoUrl: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val ref = tripsRef.child(tripId).child("galleryUrls")
        
        ref.runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                val list = currentData.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
                if (!list.contains(photoUrl)) {
                    list.add(photoUrl)
                    currentData.value = list
                }
                return com.google.firebase.database.Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) {
                    trySend(NetworkResult.Error("Lỗi thêm ảnh: ${error.message}"))
                } else {
                    trySend(NetworkResult.Success(Unit))
                }
                close()
            }
        })
        awaitClose()
    }

    override fun deleteGalleryPhoto(tripId: String, photoUrl: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val ref = tripsRef.child(tripId).child("galleryUrls")
        
        ref.runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                val list = currentData.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
                if (list.remove(photoUrl)) {
                    currentData.value = list
                }
                return com.google.firebase.database.Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) {
                    trySend(NetworkResult.Error("Lỗi xóa ảnh: ${error.message}"))
                } else {
                    trySend(NetworkResult.Success(Unit))
                }
                close()
            }
        })
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

    // --- Group Management Implementation ---

    override fun getTripMembers(tripId: String): Flow<NetworkResult<List<TripMember>>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val listener = tripMembersRef.child(tripId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val members = snapshot.children.mapNotNull { it.getValue(TripMember::class.java) }
                    trySend(NetworkResult.Success(members))
                } catch (e: Exception) {
                    android.util.Log.e("TripRepo", "Error mapping members for trip $tripId", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(NetworkResult.Error("Lỗi tải thành viên: ${error.message}"))
            }
        })
        awaitClose { tripMembersRef.child(tripId).removeEventListener(listener) }
    }

    override fun updateMemberRole(tripId: String, userId: String, role: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        tripMembersRef.child(tripId).child(userId).child("role").setValue(role)
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
                val updates = hashMapOf<String, Any>(
                    "tripMembers/$tripId/$currentOwnerId/role" to TripMember.ROLE_EDITOR,
                    "tripMembers/$tripId/$newOwnerId/role" to TripMember.ROLE_OWNER,
                    "trips/$tripId/userId" to newOwnerId
                )
                database.reference.updateChildren(updates).await()
                trySend(NetworkResult.Success(Unit))
            } catch (e: Exception) {
                trySend(NetworkResult.Error("Lỗi chuyển quyền: ${e.message}"))
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
                val updates = hashMapOf<String, Any?>(
                    "tripMembers/$tripId/$userId" to null,
                    "userTrips/$userId/$tripId" to null
                )
                database.reference.updateChildren(updates).await()
                
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
                // 1. PRIMARY SEARCH: Direct index lookup (Highest speed)
                val tripIdSnap = inviteCodesRef.child(sanitizedCode).get().await()
                var tripId = tripIdSnap.getValue(String::class.java)
                
                // 2. SECONDARY SEARCH: Direct query on 'trips' node (Reliability fallback)
                if (tripId == null) {
                    android.util.Log.w("TripRepo", "Index lookup failed, trying direct property search")
                    val querySnap = try {
                        tripsRef.orderByChild("inviteCode").equalTo(sanitizedCode).get().await()
                    } catch (e: Exception) {
                        android.util.Log.e("TripRepo", "Property search failed (likely missing index), falling back to deep scan", e)
                        null // Force step 3
                    }
                    
                    if (querySnap != null && querySnap.exists() && querySnap.childrenCount > 0) {
                        tripId = querySnap.children.first().key
                    }
                }

                // 3. FINAL SEARCH: Case-insensitive deep scan (Ultimate fallback)
                if (tripId == null) {
                    android.util.Log.w("TripRepo", "Property search failed, performing case-insensitive scan")
                    val allTripsSnap = tripsRef.get().await()
                    tripId = allTripsSnap.children.find { 
                        it.child("inviteCode").value?.toString()?.equals(sanitizedCode, ignoreCase = true) == true 
                    }?.key
                }
                
                if (tripId != null) {
                    android.util.Log.i("TripRepo", "Code $sanitizedCode resolved to trip $tripId")
                    
                    // REPAIR logic: If we found it via fallback, repair the index for future fast access
                    if (!tripIdSnap.exists()) {
                        android.util.Log.i("TripRepo", "Repairing missing inviteCodes index for $sanitizedCode")
                        inviteCodesRef.child(sanitizedCode).setValue(tripId)
                    }
                    
                    joinTripInternal(tripId, userId, userName, userPhotoUrl, this@callbackFlow)
                } else {
                    android.util.Log.e("TripRepo", "Invite code $sanitizedCode not found after triple-scan")
                    trySend(NetworkResult.Error("Mã mời không tồn tại hoặc đã hết hạn"))
                    close()
                }
            } catch (e: Exception) {
                android.util.Log.e("TripRepo", "Error joining trip via code", e)
                trySend(NetworkResult.Error("Lỗi kết nối: ${e.message}"))
                close()
            }
        }
        awaitClose()
    }

    override fun joinTripByShareToken(shareToken: String, userId: String, userName: String, userPhotoUrl: String?): Flow<NetworkResult<String>> = callbackFlow {
        trySend(NetworkResult.Loading)
        launch {
            try {
                val tripIdSnap = shareTokensRef.child(shareToken).get().await()
                val tripId = tripIdSnap.getValue(String::class.java)
                
                if (tripId != null) {
                    joinTripInternal(tripId, userId, userName, userPhotoUrl, this@callbackFlow)
                } else {
                    trySend(NetworkResult.Error("Link chia sẻ không hợp lệ"))
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
        // Check if already a member
        val memberSnap = tripMembersRef.child(tripId).child(userId).get().await()
        if (memberSnap.exists()) {
            channel.trySend(NetworkResult.Success(tripId))
            channel.close()
            return
        }

        val member = TripMember(
            userId = userId,
            tripId = tripId,
            role = TripMember.ROLE_VIEWER, // Default role
            status = TripMember.STATUS_JOINED,
            userName = userName,
            userPhotoUrl = userPhotoUrl
        )

        // Atomic update for joining
        val updates = hashMapOf<String, Any>(
            "tripMembers/$tripId/$userId" to member,
            "userTrips/$userId/$tripId" to true
        )
        database.reference.updateChildren(updates).await()
        
        channel.trySend(NetworkResult.Success(tripId))
        channel.close()
    }

    override fun leaveTrip(tripId: String, userId: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        launch {
            try {
                // Check if owner
                val memberSnap = tripMembersRef.child(tripId).child(userId).get().await()
                val member = memberSnap.getValue(TripMember::class.java)
                
                if (member?.role == TripMember.ROLE_OWNER) {
                    // Check if other members exist
                    val allMembersSnap = tripMembersRef.child(tripId).get().await()
                    if (allMembersSnap.childrenCount > 1) {
                        trySend(NetworkResult.Error("Vui lòng chuyển quyền chủ sở hữu trước khi rời nhóm"))
                        close()
                        return@launch
                    } else {
                        // Only member and owner, delete the trip atomically
                        deleteTrip(tripId).collect { result ->
                            if (result is NetworkResult.Success) {
                                trySend(NetworkResult.Success(Unit))
                                close()
                            } else if (result is NetworkResult.Error) {
                                trySend(NetworkResult.Error(result.message))
                                close()
                            }
                        }
                    }
                } else {
                    // Not owner, just remove atomically
                    val updates = hashMapOf<String, Any?>(
                        "tripMembers/$tripId/$userId" to null,
                        "userTrips/$userId/$tripId" to null
                    )
                    database.reference.updateChildren(updates).await()
                    trySend(NetworkResult.Success(Unit))
                    close()
                }
            } catch (e: Exception) {
                trySend(NetworkResult.Error(e.message ?: "Error leaving trip"))
            } finally {
                close()
            }
        }
        awaitClose()
    }

    override fun getTripLogs(tripId: String): Flow<NetworkResult<List<com.example.travelmate.domain.model.TripLog>>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val listener = tripLogsRef.child(tripId).orderByChild("timestamp").limitToLast(100).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val logs = snapshot.children.mapNotNull { it.getValue(com.example.travelmate.domain.model.TripLog::class.java) }.reversed()
                    trySend(NetworkResult.Success(logs))
                } catch (e: Exception) {
                    android.util.Log.e("TripRepo", "Error mapping trip logs", e)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(NetworkResult.Error(error.message))
            }
        })
        awaitClose { tripLogsRef.child(tripId).removeEventListener(listener) }
    }

    override fun addTripLog(log: com.example.travelmate.domain.model.TripLog): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val logId = tripLogsRef.child(log.tripId).push().key ?: UUID.randomUUID().toString()
        tripLogsRef.child(log.tripId).child(logId).setValue(log.copy(id = logId))
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
}
