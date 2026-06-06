package com.example.travelmate.data.repository

import com.example.travelmate.domain.model.Expense
import com.example.travelmate.domain.model.ItineraryDay
import com.example.travelmate.domain.model.Trip
import com.example.travelmate.domain.model.TripActivity
import com.example.travelmate.domain.repository.TripRepository
import com.example.travelmate.util.NetworkResult
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID
import javax.inject.Inject

class TripRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val storage: FirebaseStorage
) : TripRepository {

    private val tripsRef = database.getReference("trips")
    private val expensesRef = database.getReference("expenses")

    override fun createTrip(trip: Trip): Flow<NetworkResult<String>> = callbackFlow {
        trySend(NetworkResult.Loading)
        
        val tripId = if (trip.id.isEmpty()) tripsRef.push().key ?: UUID.randomUUID().toString() else trip.id
        val finalTrip = trip.copy(id = tripId)
        
        tripsRef.child(tripId).setValue(finalTrip).addOnSuccessListener {
            trySend(NetworkResult.Success(tripId))
            close()
        }.addOnFailureListener { e ->
            trySend(NetworkResult.Error("Lỗi tạo chuyến đi: ${e.message}"))
            close()
        }
        awaitClose()
    }

    override fun getUserTrips(userId: String): Flow<NetworkResult<List<Trip>>> = callbackFlow {
        trySend(NetworkResult.Loading)
        
        val listener = tripsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val list = mutableListOf<Trip>()
                    snapshot.children.forEach { child ->
                        try {
                            val trip = child.getValue(Trip::class.java)
                            if (trip != null && trip.userId == userId) {
                                val currentStatus = trip.getCalculatedStatus()
                                val updatedTrip = trip.copy(status = currentStatus)
                                list.add(updatedTrip)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("TripRepo", "Lỗi nạp chuyến đi: ${e.message}")
                        }
                    }
                    trySend(NetworkResult.Success(list.sortedByDescending { it.createdAt }))
                } catch (e: Exception) {
                    trySend(NetworkResult.Error("Lỗi đồng bộ danh sách chuyến đi"))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(NetworkResult.Error(error.message))
            }
        })
        awaitClose { tripsRef.removeEventListener(listener) }
    }

    override fun getTripById(tripId: String): Flow<NetworkResult<Trip>> = callbackFlow {
        trySend(NetworkResult.Loading)
        
        val listener = tripsRef.child(tripId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val trip = snapshot.getValue(Trip::class.java)
                    if (trip != null) {
                        trySend(NetworkResult.Success(trip))
                    } else {
                        trySend(NetworkResult.Error("Không tìm thấy chuyến đi"))
                    }
                } catch (e: Exception) {
                    trySend(NetworkResult.Error("Lỗi dữ liệu chuyến đi: ${e.message}"))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(NetworkResult.Error(error.message))
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
        
        tripsRef.child(tripId).removeValue().addOnSuccessListener {
            trySend(NetworkResult.Success(Unit))
            close()
        }.addOnFailureListener { e ->
            trySend(NetworkResult.Error("Xóa thất bại"))
            close()
        }
        awaitClose()
    }

    override fun addActivity(tripId: String, dayIndex: Int, activity: TripActivity): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        
        tripsRef.child(tripId).get().addOnSuccessListener { snapshot ->
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
                
                tripsRef.child(tripId).child("itinerary").setValue(updatedItinerary)
                    .addOnSuccessListener {
                        trySend(NetworkResult.Success(Unit))
                        close()
                    }.addOnFailureListener { e ->
                        trySend(NetworkResult.Error(e.message ?: "Lỗi thêm hoạt động"))
                        close()
                    }
            } else {
                trySend(NetworkResult.Error("Không tìm thấy Trip"))
                close()
            }
        }.addOnFailureListener { e ->
            trySend(NetworkResult.Error("Lỗi truy cập DB"))
            close()
        }
        awaitClose()
    }

    override fun toggleActivityDone(tripId: String, dayIndex: Int, activityId: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        tripsRef.child(tripId).get().addOnSuccessListener { snapshot ->
            val trip = snapshot.getValue(Trip::class.java)
            if (trip != null && dayIndex < trip.itinerary.size) {
                val updatedItinerary = trip.itinerary.toMutableList()
                val day = updatedItinerary[dayIndex]
                val updatedActivities = day.activities.map { 
                    if (it.id == activityId) it.copy(done = !it.done) else it
                }
                updatedItinerary[dayIndex] = day.copy(activities = updatedActivities)
                tripsRef.child(tripId).child("itinerary").setValue(updatedItinerary).addOnSuccessListener {
                    trySend(NetworkResult.Success(Unit))
                    close()
                }.addOnFailureListener { close() }
            } else { close() }
        }
        awaitClose()
    }

    override fun deleteActivity(tripId: String, dayIndex: Int, activityId: String): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        tripsRef.child(tripId).get().addOnSuccessListener { snapshot ->
            val trip = snapshot.getValue(Trip::class.java)
            if (trip != null && dayIndex < trip.itinerary.size) {
                val updatedItinerary = trip.itinerary.toMutableList()
                val day = updatedItinerary[dayIndex]
                val updatedActivities = day.activities.filter { it.id != activityId }
                updatedItinerary[dayIndex] = day.copy(activities = updatedActivities)
                
                tripsRef.child(tripId).child("itinerary").setValue(updatedItinerary).addOnSuccessListener {
                    trySend(NetworkResult.Success(Unit))
                    close()
                }.addOnFailureListener { close() }
            } else { close() }
        }
        awaitClose()
    }

    override fun addExpense(expense: Expense): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val expenseId = expensesRef.push().key ?: UUID.randomUUID().toString()
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

    override fun getTripExpenses(tripId: String): Flow<NetworkResult<List<Expense>>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val listener = expensesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Expense>()
                snapshot.children.forEach { child ->
                    try {
                        val expense = child.getValue(Expense::class.java)
                        if (expense != null && expense.tripId == tripId) {
                            list.add(expense)
                        }
                    } catch (e: Exception) {}
                }
                trySend(NetworkResult.Success(list))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(NetworkResult.Error(error.message))
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
        tripsRef.child(tripId).child("galleryUrls").get().addOnSuccessListener { snapshot ->
            val currentUrls = snapshot.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
            currentUrls.add(photoUrl)
            tripsRef.child(tripId).child("galleryUrls").setValue(currentUrls).addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }.addOnFailureListener { e ->
                trySend(NetworkResult.Error(e.message ?: "Error"))
                close()
            }
        }.addOnFailureListener {
            close()
        }
        awaitClose()
    }

    override fun uploadImage(path: String, byteArray: ByteArray): Flow<NetworkResult<String>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val storageRef = storage.reference.child(path)
        storageRef.putBytes(byteArray)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    trySend(NetworkResult.Success(uri.toString()))
                    close()
                }.addOnFailureListener { close() }
            }
            .addOnFailureListener { e ->
                trySend(NetworkResult.Error(e.message ?: "Upload failed"))
                close()
            }
        awaitClose()
    }
}
