package com.example.travelmate.data.repository

import android.content.Context
import com.example.travelmate.domain.model.Destination
import com.example.travelmate.domain.model.Review
import com.example.travelmate.domain.repository.AuthRepository
import com.example.travelmate.domain.repository.DestinationRepository
import com.example.travelmate.util.NetworkResult
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import com.example.travelmate.domain.repository.GeoapifyRepository
import javax.inject.Inject

class DestinationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val geoapifyRepository: GeoapifyRepository,
    @ApplicationContext private val context: Context
) : DestinationRepository {

    private val destinationsRef = firestore.collection("destinations")
    private val json = Json { ignoreUnknownKeys = true }

    private fun mapDocumentToDestination(document: DocumentSnapshot): Destination {
        val data = document.data.orEmpty()

        fun stringList(field: String): List<String> =
            (data[field] as? List<*>)?.mapNotNull { it as? String }.orEmpty()

        fun safeDouble(field: String): Double {
            return when (val value = data[field]) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
        }

        fun safeInt(field: String): Int {
            return when (val value = data[field]) {
                is Number -> value.toInt()
                is String -> value.toIntOrNull() ?: 0
                else -> 0
            }
        }

        fun safeBoolean(field: String, default: Boolean = false): Boolean {
            return when (val value = data[field]) {
                is Boolean -> value
                is String -> value.toBooleanStrictOrNull() ?: default
                else -> default
            }
        }

        return Destination(
            id = data["id"] as? String ?: document.id,
            name = data["name"] as? String ?: "",
            location = data["location"] as? String ?: "",
            country = data["country"] as? String ?: "",
            description = data["description"] as? String ?: "",
            rating = safeDouble("rating"),
            reviewCount = safeInt("reviewCount"),
            categoryTags = stringList("categoryTags"),
            imageUrls = stringList("imageUrls"),
            coverImage = data["coverImage"] as? String,
            unesco = safeBoolean("isUnesco") || safeBoolean("unesco"),
            priceLevel = safeInt("priceLevel"),
            latitude = safeDouble("latitude"),
            longitude = safeDouble("longitude"),
            promoted = safeBoolean("isPromoted") || safeBoolean("promoted"),
            avgPricePerDay = safeDouble("avgPricePerDay")
        )
    }

    override fun getDestinations(): Flow<NetworkResult<List<Destination>>> = callbackFlow {
        android.util.Log.d("DestRepository", "Listening to all destinations changes...")
        trySend(NetworkResult.Loading)

        val registration = destinationsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("DestRepository", "Firestore error: ${error.message}")
                trySend(NetworkResult.Error(error.message ?: "Loi ket noi Firestore"))
                return@addSnapshotListener
            }

            try {
                val list = snapshot?.documents?.map { mapDocumentToDestination(it) }.orEmpty()
                android.util.Log.d("DestRepository", "Data changed, found ${list.size} destinations")
                trySend(NetworkResult.Success(list))
            } catch (e: Exception) {
                android.util.Log.e("DestRepository", "Error mapping destinations", e)
                trySend(NetworkResult.Error("Loi xu ly du lieu: ${e.message}"))
            }
        }

        awaitClose { registration.remove() }
    }

    override fun getDestinationById(id: String): Flow<NetworkResult<Destination>> = callbackFlow {
        android.util.Log.d("DestRepository", "Listening to destination changes for ID: $id")

        if (id.startsWith("geoapify_")) {
            val job = launch {
                geoapifyRepository.getPlaceDetailsOnline(id).collect { result ->
                    trySend(result)
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        trySend(NetworkResult.Loading)

        val registration = destinationsRef.document(id).addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("DestRepository", "Firestore error for $id: ${error.message}")
                trySend(NetworkResult.Error(error.message ?: "Loi ket noi Firestore"))
                return@addSnapshotListener
            }

            try {
                if (snapshot?.exists() == true) {
                    trySend(NetworkResult.Success(mapDocumentToDestination(snapshot)))
                } else {
                    launch {
                        try {
                            val searchSnap = destinationsRef.whereEqualTo("id", id).limit(1).get().await()
                            val doc = searchSnap.documents.firstOrNull()
                            if (doc != null) {
                                trySend(NetworkResult.Success(mapDocumentToDestination(doc)))
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
                trySend(NetworkResult.Error("Loi du lieu: ${e.message}"))
            }
        }

        awaitClose { registration.remove() }
    }

    override fun getRecommendedDestinations(limit: Int): Flow<NetworkResult<List<Destination>>> = callbackFlow {
        android.util.Log.d("DestRepository", "Listening to recommended destinations...")
        trySend(NetworkResult.Loading)

        val registration = destinationsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("DestRepository", "Recommended destinations cancelled: ${error.message}")
                trySend(NetworkResult.Error(error.message ?: "Loi ket noi Firestore"))
                return@addSnapshotListener
            }

            launch {
                try {
                    val user = authRepository.getCurrentUser()
                    val userInterests = user?.interests ?: emptyList()
                    val list = snapshot?.documents?.map { mapDocumentToDestination(it) }.orEmpty()

                    val result = list
                        .filter { destination -> userInterests.isEmpty() || destination.categories.any { it in userInterests } }
                        .sortedByDescending { destination -> destination.categories.count { it in userInterests } }
                        .take(limit)

                    trySend(NetworkResult.Success(result))
                } catch (e: Exception) {
                    android.util.Log.e("DestRepository", "Error in recommended destinations", e)
                    trySend(NetworkResult.Error("Loi goi y: ${e.message}"))
                }
            }
        }

        awaitClose { registration.remove() }
    }

    override fun seedDatabase(): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        launch {
            try {
                val jsonString = context.assets.open("data/seed_data.json")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                val root = json.parseToJsonElement(jsonString).jsonObject
                val destinationsJson = root["destinations"]?.jsonObject

                if (destinationsJson == null || destinationsJson.isEmpty()) {
                    trySend(NetworkResult.Error("Khong tim thay du lieu trong file seed"))
                    close()
                    return@launch
                }

                val batch = firestore.batch()
                destinationsJson.forEach { (key, value) ->
                    val destination = json.decodeFromJsonElement<Destination>(value)
                    batch.set(destinationsRef.document(key), destination.copy(id = destination.id.ifEmpty { key }))
                }

                val reviewsJson = root["reviews"]?.jsonObject
                reviewsJson?.forEach { (destId, destReviewsElement) ->
                    val reviewsObj = destReviewsElement.jsonObject
                    reviewsObj.forEach { (reviewId, reviewValue) ->
                        val review = json.decodeFromJsonElement<Review>(reviewValue)
                        val finalReview = review.copy(id = reviewId.ifEmpty { review.id }, destinationId = destId)
                        batch.set(firestore.collection("reviews").document(destId).collection("items").document(finalReview.id), finalReview)
                        if (finalReview.userId.isNotEmpty()) {
                            batch.set(firestore.collection("user_reviews").document(finalReview.userId).collection("items").document(finalReview.id), finalReview)
                        }
                    }
                }

                batch.commit().await()
                trySend(NetworkResult.Success(Unit))
            } catch (e: Exception) {
                trySend(NetworkResult.Error("Loi nap du lieu: ${e.message}"))
            } finally {
                close()
            }
        }
        awaitClose()
    }

    override fun searchPlacesGeoapify(query: String): Flow<NetworkResult<List<Destination>>> {
        return geoapifyRepository.searchPlacesOnline(query)
    }

    private fun JsonElement.toAny(): Any? {
        return when (this) {
            is JsonPrimitive -> {
                if (isString) content else booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
            }
            is JsonObject -> mapValues { it.value.toAny() }
            is JsonArray -> map { it.toAny() }
        }
    }
}
