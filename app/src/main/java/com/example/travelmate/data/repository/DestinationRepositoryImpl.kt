package com.example.travelmate.data.repository

import android.content.Context
import com.example.travelmate.data.local.DestinationDao
import com.example.travelmate.data.model.toDomainModel
import com.example.travelmate.domain.model.Destination
import com.example.travelmate.domain.repository.AuthRepository
import com.example.travelmate.domain.repository.DestinationRepository
import com.example.travelmate.domain.repository.GeoapifyRepository
import com.example.travelmate.util.NetworkResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DestinationRepositoryImpl @Inject constructor(
    private val geoapifyRepository: GeoapifyRepository,
    private val destinationDao: DestinationDao,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : DestinationRepository {

    override fun getDestinations(): Flow<NetworkResult<List<Destination>>> = flow {
        emit(NetworkResult.Loading)
        val initialEntities = destinationDao.getAllDestinationsSync()
        if (initialEntities.isEmpty()) {
            seedPopularPlacesIntoRoom()
        }
        destinationDao.getAllDestinations().collect { entities ->
            emit(NetworkResult.Success(entities.map { it.toDomainModel() }))
        }
    }.flowOn(Dispatchers.IO)

    override fun getDestinationById(id: String): Flow<NetworkResult<Destination>> {
        return geoapifyRepository.getPlaceDetailsOnline(id)
    }

    override fun getRecommendedDestinations(limit: Int): Flow<NetworkResult<List<Destination>>> = flow {
        emit(NetworkResult.Loading)
        val initialEntities = destinationDao.getTopDestinationsSync()
        if (initialEntities.isEmpty()) {
            seedPopularPlacesIntoRoom()
        }
        destinationDao.getTopDestinations().collect { entities ->
            emit(NetworkResult.Success(entities.map { it.toDomainModel() }.take(limit)))
        }
    }.flowOn(Dispatchers.IO)

    override fun seedDatabase(): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        seedPopularPlacesIntoRoom()
        emit(NetworkResult.Success(Unit))
    }.flowOn(Dispatchers.IO)

    override fun searchPlacesGeoapify(query: String): Flow<NetworkResult<List<Destination>>> {
        return geoapifyRepository.searchPlacesOnline(query)
    }

    private suspend fun seedPopularPlacesIntoRoom() {
        val popularKeywords = listOf("Hanoi", "Da Nang", "Ho Chi Minh City", "Da Lat", "Nha Trang", "Phu Quoc")
        popularKeywords.forEach { keyword ->
            try {
                geoapifyRepository.searchPlacesOnline(keyword).collect { }
            } catch (e: Exception) {
                android.util.Log.e("DestRepository", "Error seeding $keyword into Room", e)
            }
        }
    }
}
