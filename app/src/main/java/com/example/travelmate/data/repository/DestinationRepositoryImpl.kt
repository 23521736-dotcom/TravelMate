package com.example.travelmate.data.repository

import android.content.Context
import com.example.travelmate.data.local.DayForecastModel
import com.example.travelmate.data.local.DestinationDao
import com.example.travelmate.data.local.DestinationEntity
import com.example.travelmate.data.local.WeatherDao
import com.example.travelmate.data.local.WeatherForecastEntity
import com.example.travelmate.data.model.toDomainModel
import com.example.travelmate.data.remote.WeatherApiService
import com.example.travelmate.data.remote.WikipediaApiService
import com.example.travelmate.domain.model.Destination
import com.example.travelmate.domain.model.DestinationDetailBundle
import com.example.travelmate.domain.repository.AuthRepository
import com.example.travelmate.domain.repository.DestinationRepository
import com.example.travelmate.domain.repository.GeoapifyRepository
import com.example.travelmate.util.NetworkResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DestinationRepositoryImpl @Inject constructor(
    private val geoapifyRepository: GeoapifyRepository,
    private val destinationDao: DestinationDao,
    private val weatherDao: WeatherDao,
    private val weatherApiService: WeatherApiService,
    private val wikipediaApiService: WikipediaApiService,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : DestinationRepository {

    override fun getDestinations(): Flow<NetworkResult<List<Destination>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val initialEntities = destinationDao.getAllDestinationsSync()
            if (initialEntities.isEmpty()) {
                seedPopularPlacesIntoRoom()
            }
        } catch (e: Exception) {
            android.util.Log.e("DestRepository", "Error checking initial Room entities", e)
        }

        destinationDao.getAllDestinations()
            .map<List<DestinationEntity>, NetworkResult<List<Destination>>> { entities ->
                NetworkResult.Success(entities.map { it.toDomainModel() })
            }
            .catch { e ->
                android.util.Log.e("DestRepository", "Error in getAllDestinations flow", e)
                emit(NetworkResult.Success(emptyList()))
            }
            .collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    override fun getDestinationById(id: String): Flow<NetworkResult<Destination>> {
        return geoapifyRepository.getPlaceDetailsOnline(id)
    }

    override fun getDestinationDetail(
        destinationId: String,
        name: String,
        lat: Double,
        lon: Double
    ): Flow<NetworkResult<DestinationDetailBundle>> = flow {
        emit(NetworkResult.Loading)
        val now = System.currentTimeMillis()

        // 1. Read cached data from Room DB
        val localDest = try {
            destinationDao.getDestinationByIdSync(destinationId)
        } catch (_: Exception) {
            null
        }

        val localWeather = try {
            weatherDao.getWeatherByDestinationIdSync(destinationId)
        } catch (_: Exception) {
            null
        }

        // Emit local data immediately if available
        if (localDest != null) {
            emit(NetworkResult.Success(DestinationDetailBundle(localDest, localWeather, emptyList())))
        }

        // 2. Fetch network updates in parallel using async / coroutineScope
        var nearbyList = emptyList<Destination>()
        coroutineScope {
            // Wikipedia Branch (refresh if missing or older than 7 days)
            val wikiDeferred = async {
                val shouldUpdateWiki = localDest == null ||
                        localDest.wikiDescription.isNullOrBlank() ||
                        (now - localDest.wikiLastUpdated) > (7 * 24 * 3600 * 1000L)
                if (shouldUpdateWiki && name.isNotBlank()) {
                    try {
                        val searchTitle = name.replace("Thành phố ", "").replace("TP. ", "").trim()
                        val resp = wikipediaApiService.getPageSummary(searchTitle)
                        val extractText = resp.extract
                        val pageUrl = resp.contentUrls?.desktop?.page
                        if (!extractText.isNullOrBlank()) {
                            destinationDao.updateWikiInfo(destinationId, extractText, pageUrl, now)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("DestRepository", "Wiki update failed: ${e.message}")
                    }
                }
            }

            // Weather Branch (refresh if missing or older than 2 hours)
            val weatherDeferred = async {
                val shouldUpdateWeather = localWeather == null || (now - localWeather.lastUpdated) > (2 * 3600 * 1000L)
                if (shouldUpdateWeather && lat != 0.0 && lon != 0.0) {
                    try {
                        val resp = weatherApiService.getWeatherForecast(latitude = lat, longitude = lon)
                        val current = resp.current
                        val daily = resp.daily
                        if (current != null) {
                            val daysModel = mutableListOf<DayForecastModel>()
                            if (daily != null && daily.time.isNotEmpty()) {
                                for (i in 0 until minOf(3, daily.time.size)) {
                                    daysModel.add(
                                        DayForecastModel(
                                            date = daily.time.getOrElse(i) { "" },
                                            minTemp = daily.tempMin.getOrElse(i) { 24.0 },
                                            maxTemp = daily.tempMax.getOrElse(i) { 31.0 },
                                            weatherCode = daily.weatherCode.getOrElse(i) { current.weatherCode }
                                        )
                                    )
                                }
                            }
                            val weatherEntity = WeatherForecastEntity(
                                destinationId = destinationId,
                                currentTemp = current.temperature,
                                currentWeatherCode = current.weatherCode,
                                humidity = 75,
                                forecastDays = daysModel,
                                lastUpdated = now
                            )
                            weatherDao.insertOrUpdateWeather(weatherEntity)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("DestRepository", "Weather update failed: ${e.message}")
                    }
                }
            }

            // Nearby Places Branch
            val nearbyDeferred = async {
                if (lat != 0.0 && lon != 0.0) {
                    try {
                        geoapifyRepository.getNearbyPlacesOnline(lat, lon, limit = 8).collect { res ->
                            if (res is NetworkResult.Success) {
                                nearbyList = res.data
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("DestRepository", "Nearby places failed: ${e.message}")
                    }
                }
            }

            wikiDeferred.await()
            weatherDeferred.await()
            nearbyDeferred.await()
        }

        // Re-read updated local DB entities and emit final bundle
        val finalDest = try {
            destinationDao.getDestinationByIdSync(destinationId) ?: localDest
        } catch (_: Exception) {
            localDest
        }

        val finalWeather = try {
            weatherDao.getWeatherByDestinationIdSync(destinationId) ?: localWeather
        } catch (_: Exception) {
            localWeather
        }

        if (finalDest != null) {
            emit(NetworkResult.Success(DestinationDetailBundle(finalDest, finalWeather, nearbyList)))
        } else {
            emit(NetworkResult.Error("Không thể tải thông tin địa điểm."))
        }
    }.flowOn(Dispatchers.IO)

    override fun getRecommendedDestinations(limit: Int): Flow<NetworkResult<List<Destination>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val initialEntities = destinationDao.getTopDestinationsSync()
            if (initialEntities.isEmpty()) {
                seedPopularPlacesIntoRoom()
            }
        } catch (e: Exception) {
            android.util.Log.e("DestRepository", "Error checking top destinations", e)
        }

        destinationDao.getTopDestinations()
            .map<List<DestinationEntity>, NetworkResult<List<Destination>>> { entities ->
                NetworkResult.Success(entities.map { it.toDomainModel() }.take(limit))
            }
            .catch { e ->
                android.util.Log.e("DestRepository", "Error in getRecommendedDestinations flow", e)
                emit(NetworkResult.Success(emptyList()))
            }
            .collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    override fun getSavedOfflineDestinations(): Flow<NetworkResult<List<Destination>>> {
        return destinationDao.getSavedOfflineDestinations()
            .map<List<DestinationEntity>, NetworkResult<List<Destination>>> { entities ->
                NetworkResult.Success(entities.map { it.toDomainModel() })
            }
            .catch { e ->
                android.util.Log.e("DestRepository", "Error in getSavedOfflineDestinations flow", e)
                emit(NetworkResult.Success(emptyList()))
            }
            .flowOn(Dispatchers.IO)
    }

    override fun toggleOfflineSavedStatus(destinationId: String, isSaved: Boolean): Flow<NetworkResult<Boolean>> = flow {
        emit(NetworkResult.Loading)
        try {
            destinationDao.updateOfflineSavedStatus(destinationId, isSaved)
            emit(NetworkResult.Success(isSaved))
        } catch (e: Exception) {
            emit(NetworkResult.Error("Lỗi lưu offline: ${e.message}"))
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
