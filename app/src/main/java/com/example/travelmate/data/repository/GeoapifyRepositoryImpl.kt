package com.example.travelmate.data.repository

import com.example.travelmate.data.model.toDestination
import com.example.travelmate.data.remote.GeoapifyApiService
import com.example.travelmate.domain.model.Destination
import com.example.travelmate.domain.repository.GeoapifyRepository
import com.example.travelmate.util.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeoapifyRepositoryImpl @Inject constructor(
    private val apiService: GeoapifyApiService
) : GeoapifyRepository {

    private val apiKey = "fc415480b8b0473790661a20b28ac855"
    private val cache = ConcurrentHashMap<String, Destination>()

    override fun searchPlacesOnline(query: String): Flow<NetworkResult<List<Destination>>> = flow {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            emit(NetworkResult.Success(emptyList()))
            return@flow
        }
        emit(NetworkResult.Loading)
        try {
            val response = apiService.searchPlaces(text = trimmed, apiKey = apiKey, limit = 20, lang = "vi")
            val destinations = response.features.map { feature ->
                val dest = feature.toDestination(apiKey)
                cache[dest.id] = dest
                dest
            }
            emit(NetworkResult.Success(destinations))
        } catch (e: Exception) {
            android.util.Log.e("GeoapifyRepo", "Error searching Geoapify", e)
            emit(NetworkResult.Error(e.localizedMessage ?: e.message ?: "Lỗi kết nối Geoapify API"))
        }
    }

    override fun getPlaceDetailsOnline(placeId: String): Flow<NetworkResult<Destination>> = flow {
        val cached = cache[placeId]
        if (cached != null) {
            emit(NetworkResult.Success(cached))
        } else {
            emit(NetworkResult.Loading)
        }

        val rawPlaceId = placeId.removePrefix("geoapify_")
        try {
            val response = apiService.getPlaceDetails(id = rawPlaceId, apiKey = apiKey, lang = "vi")
            val feature = response.features.firstOrNull()
            if (feature != null) {
                val dest = feature.toDestination(apiKey)
                val finalDest = dest.copy(id = placeId)
                cache[placeId] = finalDest
                emit(NetworkResult.Success(finalDest))
            } else if (cached != null) {
                emit(NetworkResult.Success(cached))
            } else {
                emit(NetworkResult.Error("Không tìm thấy thông tin chi tiết địa điểm từ Geoapify"))
            }
        } catch (e: Exception) {
            android.util.Log.e("GeoapifyRepo", "Error fetching place details", e)
            if (cached != null) {
                emit(NetworkResult.Success(cached))
            } else {
                emit(NetworkResult.Error(e.localizedMessage ?: e.message ?: "Lỗi lấy chi tiết địa điểm Geoapify"))
            }
        }
    }

    override fun getCachedDestination(id: String): Destination? {
        return cache[id]
    }
}
