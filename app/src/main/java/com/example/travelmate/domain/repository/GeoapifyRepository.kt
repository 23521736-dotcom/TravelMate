package com.example.travelmate.domain.repository

import com.example.travelmate.domain.model.Destination
import com.example.travelmate.util.NetworkResult
import kotlinx.coroutines.flow.Flow

interface GeoapifyRepository {
    fun searchPlacesOnline(query: String): Flow<NetworkResult<List<Destination>>>
    fun getPlaceDetailsOnline(placeId: String): Flow<NetworkResult<Destination>>
    fun getCachedDestination(id: String): Destination?
}
