package com.example.travelmate.domain.repository

import com.example.travelmate.domain.model.Destination
import com.example.travelmate.domain.model.DestinationDetailBundle
import com.example.travelmate.util.NetworkResult
import kotlinx.coroutines.flow.Flow

interface DestinationRepository {
    fun getDestinations(): Flow<NetworkResult<List<Destination>>>
    fun getDestinationById(id: String): Flow<NetworkResult<Destination>>
    fun getRecommendedDestinations(limit: Int = 10): Flow<NetworkResult<List<Destination>>>
    fun getSavedOfflineDestinations(): Flow<NetworkResult<List<Destination>>>
    fun seedDatabase(): Flow<NetworkResult<Unit>>
    fun searchPlacesGeoapify(query: String): Flow<NetworkResult<List<Destination>>>
    fun getDestinationDetail(destinationId: String, name: String, lat: Double, lon: Double): Flow<NetworkResult<DestinationDetailBundle>>
    fun toggleOfflineSavedStatus(destinationId: String, isSaved: Boolean): Flow<NetworkResult<Boolean>>
}
