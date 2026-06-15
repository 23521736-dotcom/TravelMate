package com.example.travelmate.data.repository

import android.content.Context
import com.example.travelmate.domain.model.Destination
import com.example.travelmate.domain.repository.AuthRepository
import com.example.travelmate.domain.repository.DestinationRepository
import com.example.travelmate.util.NetworkResult
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.Json
import javax.inject.Inject

class DestinationRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : DestinationRepository {

    private val destinationsRef = database.getReference("destinations")
    private val json = Json { ignoreUnknownKeys = true }

    private fun mapSnapshotToDestination(snapshot: DataSnapshot): Destination {
        val categoryTags = mutableListOf<String>()
        snapshot.child("categoryTags").children.forEach { child ->
            child.getValue(String::class.java)?.let { categoryTags.add(it) }
        }
        
        val imageUrls = mutableListOf<String>()
        snapshot.child("imageUrls").children.forEach { child ->
            child.getValue(String::class.java)?.let { imageUrls.add(it) }
        }

        // Safer number conversion
        fun safeDouble(snap: DataSnapshot): Double {
            return when (val v = snap.value) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
        }

        fun safeInt(snap: DataSnapshot): Int {
            return when (val v = snap.value) {
                is Number -> v.toInt()
                is String -> v.toIntOrNull() ?: 0
                else -> 0
            }
        }

        fun safeBoolean(snap: DataSnapshot, default: Boolean = false): Boolean {
            return when (val v = snap.value) {
                is Boolean -> v
                is String -> v.toBoolean()
                else -> default
            }
        }

        return Destination(
            id = snapshot.child("id").getValue(String::class.java) ?: snapshot.key ?: "",
            name = snapshot.child("name").getValue(String::class.java) ?: "",
            location = snapshot.child("location").getValue(String::class.java) ?: "",
            country = snapshot.child("country").getValue(String::class.java) ?: "",
            description = snapshot.child("description").getValue(String::class.java) ?: "",
            rating = safeDouble(snapshot.child("rating")),
            reviewCount = safeInt(snapshot.child("reviewCount")),
            categoryTags = categoryTags,
            imageUrls = imageUrls,
            coverImage = snapshot.child("coverImage").getValue(String::class.java),
            unesco = safeBoolean(snapshot.child("isUnesco")) || safeBoolean(snapshot.child("unesco")),
            priceLevel = safeInt(snapshot.child("priceLevel")),
            latitude = safeDouble(snapshot.child("latitude")),
            longitude = safeDouble(snapshot.child("longitude")),
            promoted = safeBoolean(snapshot.child("isPromoted")) || safeBoolean(snapshot.child("promoted")),
            avgPricePerDay = safeDouble(snapshot.child("avgPricePerDay"))
        )
    }

    override fun getDestinations(): Flow<NetworkResult<List<Destination>>> = callbackFlow {
        android.util.Log.d("DestRepository", "Listening to all destinations changes...")
        trySend(NetworkResult.Loading)
        
        val listener = destinationsRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val list = mutableListOf<Destination>()
                    if (snapshot.exists()) {
                        android.util.Log.d("DestRepository", "Data changed, found ${snapshot.childrenCount} destinations")
                        snapshot.children.forEach { child ->
                            list.add(mapSnapshotToDestination(child))
                        }
                    } else {
                        android.util.Log.w("DestRepository", "No destinations found in database")
                    }
                    trySend(NetworkResult.Success(list))
                } catch (e: Exception) {
                    android.util.Log.e("DestRepository", "Error mapping destinations", e)
                    trySend(NetworkResult.Error("Lỗi xử lý dữ liệu: ${e.message}"))
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                android.util.Log.e("DestRepository", "Database error: ${error.message}")
                trySend(NetworkResult.Error(error.message))
            }
        })
        
        awaitClose { destinationsRef.removeEventListener(listener) }
    }

    override fun getDestinationById(id: String): Flow<NetworkResult<Destination>> = callbackFlow {
        android.util.Log.d("DestRepository", "Listening to destination changes for ID: $id")
        trySend(NetworkResult.Loading)
        
        val listener = destinationsRef.child(id).addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    if (snapshot.exists()) {
                        android.util.Log.d("DestRepository", "Destination updated: ${snapshot.child("name").value}")
                        trySend(NetworkResult.Success(mapSnapshotToDestination(snapshot)))
                    } else {
                        android.util.Log.w("DestRepository", "Destination with ID $id not found")
                        // If ID not found directly, try searching by child property "id"
                        launch {
                            try {
                                val searchSnap = destinationsRef.orderByChild("id").equalTo(id).get().await()
                                if (searchSnap.exists() && searchSnap.childrenCount > 0) {
                                    trySend(NetworkResult.Success(mapSnapshotToDestination(searchSnap.children.first())))
                                } else {
                                    trySend(NetworkResult.Error("Destination not found"))
                                }
                            } catch (e: Exception) {
                                trySend(NetworkResult.Error("Destination search failed"))
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DestRepository", "Error mapping destination $id", e)
                    trySend(NetworkResult.Error("Lỗi dữ liệu: ${e.message}"))
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                android.util.Log.e("DestRepository", "Database error for $id: ${error.message}")
                trySend(NetworkResult.Error(error.message))
            }
        })
        
        awaitClose { destinationsRef.child(id).removeEventListener(listener) }
    }

    override fun getRecommendedDestinations(limit: Int): Flow<NetworkResult<List<Destination>>> = callbackFlow {
        android.util.Log.d("DestRepository", "Listening to recommended destinations...")
        trySend(NetworkResult.Loading)
        
        val listener = destinationsRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                launch {
                    try {
                        val user = authRepository.getCurrentUser()
                        val userInterests = user?.interests ?: emptyList()
                        
                        val list = mutableListOf<Destination>()
                        if (snapshot.exists()) {
                            snapshot.children.forEach { child ->
                                val dest = mapSnapshotToDestination(child)
                                if (userInterests.isEmpty() || dest.categories.any { it in userInterests }) {
                                    list.add(dest)
                                }
                            }
                        }
                        
                        val result = list.sortedByDescending { dest -> 
                            dest.categories.count { it in userInterests }
                        }.take(limit)
                        
                        trySend(NetworkResult.Success(result))
                    } catch (e: Exception) {
                        android.util.Log.e("DestRepository", "Error in recommended destinations", e)
                        trySend(NetworkResult.Error("Lỗi gợi ý: ${e.message}"))
                    }
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                android.util.Log.e("DestRepository", "Recommended destinations cancelled: ${error.message}")
                trySend(NetworkResult.Error(error.message))
            }
        })
        
        awaitClose { destinationsRef.removeEventListener(listener) }
    }

    override fun seedDatabase(): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        try {
            val inputStream = context.assets.open("data/seed_data.json")
            val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = json.parseToJsonElement(jsonString).jsonObject
            val destinationsJson = root["destinations"]?.jsonObject
            
            if (destinationsJson == null || destinationsJson.isEmpty()) {
                trySend(NetworkResult.Error("Không tìm thấy dữ liệu trong file seed"))
                close()
                return@callbackFlow
            }

            val total = destinationsJson.size
            var count = 0
            
            destinationsRef.removeValue().addOnCompleteListener {
                destinationsJson.forEach { (key, value) ->
                    try {
                        val dest = json.decodeFromJsonElement<Destination>(value)
                        destinationsRef.child(key).setValue(dest).addOnCompleteListener {
                            count++
                            if (count == total) {
                                trySend(NetworkResult.Success(Unit))
                                close()
                            }
                        }
                    } catch (e: Exception) {
                        count++
                        if (count == total) {
                            trySend(NetworkResult.Success(Unit))
                            close()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            trySend(NetworkResult.Error("Lỗi nạp dữ liệu: ${e.message}"))
            close()
        }
        awaitClose()
    }
}
