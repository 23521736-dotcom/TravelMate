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

        return Destination(
            id = snapshot.child("id").getValue(String::class.java) ?: snapshot.key ?: "",
            name = snapshot.child("name").getValue(String::class.java) ?: "",
            location = snapshot.child("location").getValue(String::class.java) ?: "",
            country = snapshot.child("country").getValue(String::class.java) ?: "",
            description = snapshot.child("description").getValue(String::class.java) ?: "",
            rating = snapshot.child("rating").getValue(Number::class.java)?.toDouble() ?: 0.0,
            reviewCount = snapshot.child("reviewCount").getValue(Number::class.java)?.toInt() ?: 0,
            categoryTags = categoryTags,
            imageUrls = imageUrls,
            unesco = snapshot.child("isUnesco").getValue(Boolean::class.java) 
                ?: snapshot.child("unesco").getValue(Boolean::class.java) ?: false,
            priceLevel = snapshot.child("priceLevel").getValue(Number::class.java)?.toInt() ?: 2,
            latitude = snapshot.child("latitude").getValue(Number::class.java)?.toDouble() ?: 0.0,
            longitude = snapshot.child("longitude").getValue(Number::class.java)?.toDouble() ?: 0.0,
            promoted = snapshot.child("isPromoted").getValue(Boolean::class.java)
                ?: snapshot.child("promoted").getValue(Boolean::class.java) ?: false,
            avgPricePerDay = snapshot.child("avgPricePerDay").getValue(Number::class.java)?.toDouble() ?: 0.0
        )
    }

    override fun getDestinations(): Flow<NetworkResult<List<Destination>>> = callbackFlow {
        trySend(NetworkResult.Loading)
        destinationsRef.get().addOnSuccessListener { snapshot ->
            try {
                val list = mutableListOf<Destination>()
                snapshot.children.forEach { child ->
                    list.add(mapSnapshotToDestination(child))
                }
                trySend(NetworkResult.Success(list))
            } catch (e: Exception) {
                trySend(NetworkResult.Error("Lỗi xử lý dữ liệu: ${e.message}"))
            }
            close()
        }.addOnFailureListener { e ->
            trySend(NetworkResult.Error(e.message ?: "Failed to fetch destinations"))
            close()
        }
        awaitClose()
    }

    override fun getDestinationById(id: String): Flow<NetworkResult<Destination>> = callbackFlow {
        trySend(NetworkResult.Loading)
        destinationsRef.child(id).get().addOnSuccessListener { snapshot ->
            try {
                if (snapshot.exists()) {
                    trySend(NetworkResult.Success(mapSnapshotToDestination(snapshot)))
                } else {
                    trySend(NetworkResult.Error("Destination not found"))
                }
            } catch (e: Exception) {
                trySend(NetworkResult.Error("Lỗi dữ liệu: ${e.message}"))
            }
            close()
        }.addOnFailureListener { e ->
            trySend(NetworkResult.Error(e.message ?: "Error"))
            close()
        }
        awaitClose()
    }

    override fun getRecommendedDestinations(limit: Int): Flow<NetworkResult<List<Destination>>> = callbackFlow {
        trySend(NetworkResult.Loading)
        val user = authRepository.getCurrentUser()
        val userInterests = user?.interests ?: emptyList()

        destinationsRef.get().addOnSuccessListener { snapshot ->
            try {
                val list = mutableListOf<Destination>()
                snapshot.children.forEach { child ->
                    val dest = mapSnapshotToDestination(child)
                    if (userInterests.isEmpty() || dest.categories.any { it in userInterests }) {
                        list.add(dest)
                    }
                }
                
                val result = list.sortedByDescending { dest -> 
                    dest.categories.count { it in userInterests }
                }.take(limit)
                
                trySend(NetworkResult.Success(result))
            } catch (e: Exception) {
                trySend(NetworkResult.Error("Lỗi gợi ý: ${e.message}"))
            }
            close()
        }.addOnFailureListener { e ->
            trySend(NetworkResult.Error(e.message ?: "Error"))
            close()
        }
        awaitClose()
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
            
            // Xóa dữ liệu cũ trước khi nạp mới để đảm bảo sạch sẽ
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
