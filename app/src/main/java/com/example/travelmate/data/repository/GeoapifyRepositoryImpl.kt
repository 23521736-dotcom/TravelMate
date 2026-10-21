package com.example.travelmate.data.repository

import com.example.travelmate.BuildConfig
import com.example.travelmate.data.local.DestinationDao
import com.example.travelmate.data.model.toDomainModel
import com.example.travelmate.data.model.toEntity
import com.example.travelmate.data.model.toDestination
import com.example.travelmate.data.remote.GeoapifyApiService
import com.example.travelmate.data.remote.ImageSearchApiService
import com.example.travelmate.domain.model.Destination
import com.example.travelmate.domain.repository.GeoapifyRepository
import com.example.travelmate.util.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeoapifyRepositoryImpl @Inject constructor(
    private val apiService: GeoapifyApiService,
    private val imageApiService: ImageSearchApiService,
    private val destinationDao: DestinationDao
) : GeoapifyRepository {

    private val geoapifyKey = BuildConfig.GEOAPIFY_API_KEY.ifEmpty { "fc415480b8b0473790661a20b28ac855" }
    private val unsplashKey = BuildConfig.UNSPLASH_ACCESS_KEY
    private val pixabayKey = BuildConfig.PIXABAY_API_KEY

    override fun searchPlacesOnline(query: String): Flow<NetworkResult<List<Destination>>> = flow {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            emit(NetworkResult.Success(emptyList()))
            return@flow
        }
        emit(NetworkResult.Loading)
        try {
            val response = apiService.searchPlaces(text = trimmed, apiKey = geoapifyKey, limit = 10, lang = "vi")
            val destinations = response.features.map { feature ->
                // First get basic destination to get the name for image search
                val basicDest = feature.toDestination(geoapifyKey)
                
                // Fetch image from Unsplash/Pixabay
                val photoUrl = fetchImageForPlace(basicDest.name)
                
                // Re-create destination with real photo
                val fullDest = feature.toDestination(geoapifyKey, photoUrl)
                
                // Cache into Room database
                destinationDao.insertDestination(fullDest.toEntity())
                
                fullDest
            }
            emit(NetworkResult.Success(destinations))
        } catch (e: Exception) {
            android.util.Log.e("GeoapifyRepo", "Error searching Geoapify", e)
            emit(NetworkResult.Error(e.localizedMessage ?: e.message ?: "Lỗi kết nối Geoapify API"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getPlaceDetailsOnline(placeId: String): Flow<NetworkResult<Destination>> = flow {
        // Try getting from Room DB first
        val cachedEntity = destinationDao.getDestinationByIdSync(placeId)
        if (cachedEntity != null) {
            emit(NetworkResult.Success(cachedEntity.toDomainModel()))
        } else {
            emit(NetworkResult.Loading)
        }

        val rawPlaceId = placeId.removePrefix("geoapify_")
        try {
            val response = apiService.getPlaceDetails(id = rawPlaceId, apiKey = geoapifyKey, lang = "vi")
            val feature = response.features.firstOrNull()
            if (feature != null) {
                val basicDest = feature.toDestination(geoapifyKey)
                val finalDest = basicDest.copy(id = placeId)
                
                // If it's a new place (not in DB), fetch image and save
                if (cachedEntity == null) {
                    val photoUrl = fetchImageForPlace(finalDest.name)
                    val fullDest = feature.toDestination(geoapifyKey, photoUrl).copy(id = placeId)
                    destinationDao.insertDestination(fullDest.toEntity())
                    emit(NetworkResult.Success(fullDest))
                } else {
                    emit(NetworkResult.Success(finalDest)) // Already cached
                }
            } else if (cachedEntity != null) {
                emit(NetworkResult.Success(cachedEntity.toDomainModel()))
            } else {
                emit(NetworkResult.Error("Không tìm thấy thông tin chi tiết địa điểm từ Geoapify"))
            }
        } catch (e: Exception) {
            android.util.Log.e("GeoapifyRepo", "Error fetching place details", e)
            if (cachedEntity != null) {
                emit(NetworkResult.Success(cachedEntity.toDomainModel()))
            } else {
                emit(NetworkResult.Error(e.localizedMessage ?: e.message ?: "Lỗi lấy chi tiết địa điểm Geoapify"))
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun getCachedDestination(id: String): Destination? {
        return null
    }

    private suspend fun fetchImageForPlace(placeName: String): String? {
        val searchQuery = "$placeName travel panorama" // Magic keyword to get wide scenic shots
        
        // Try Unsplash first
        try {
            val unsplashResp = imageApiService.searchUnsplash(
                query = searchQuery,
                clientId = unsplashKey
            )
            val url = unsplashResp.results.firstOrNull()?.urls?.regular
            if (!url.isNullOrEmpty()) return url
        } catch (e: Exception) {
            android.util.Log.w("GeoapifyRepo", "Unsplash API failed: ${e.message}")
        }
        
        // Fallback to Pixabay
        try {
            val pixabayResp = imageApiService.searchPixabay(
                query = searchQuery,
                apiKey = pixabayKey
            )
            val url = pixabayResp.hits.firstOrNull()?.largeImageURL
            if (!url.isNullOrEmpty()) return url
        } catch (e: Exception) {
            android.util.Log.w("GeoapifyRepo", "Pixabay API failed: ${e.message}")
        }
        
        return null // Return null if both fail, the toDestination mapper will use curated default
    }
}
