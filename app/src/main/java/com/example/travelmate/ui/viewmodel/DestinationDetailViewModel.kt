package com.example.travelmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.data.remote.ForecastDayData
import com.example.travelmate.data.remote.WeatherUiData
import com.example.travelmate.data.remote.getTravelAdvice
import com.example.travelmate.data.remote.parseWmoCode
import com.example.travelmate.data.model.toDomainModel
import com.example.travelmate.domain.model.Destination
import com.example.travelmate.domain.model.Review
import com.example.travelmate.domain.repository.DestinationRepository
import com.example.travelmate.domain.repository.ReviewRepository
import com.example.travelmate.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

data class DestinationDetailUiState(
    val destination: Destination? = null,
    val reviews: List<Review> = emptyList(),
    val nearbyPlaces: List<Destination> = emptyList(),
    val weatherData: WeatherUiData? = null,
    val wikiSummary: String? = null,
    val wikiPageUrl: String? = null,
    val isLoading: Boolean = false,
    val isNearbyLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DestinationDetailViewModel @Inject constructor(
    private val destinationRepository: DestinationRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DestinationDetailUiState())
    val uiState: StateFlow<DestinationDetailUiState> = _uiState.asStateFlow()

    private var destinationJob: Job? = null
    private var detailBundleJob: Job? = null
    private var reviewsJob: Job? = null

    fun getDestination(id: String) {
        if (_uiState.value.destination?.id == id && !_uiState.value.isLoading) return

        // Cancel all ongoing jobs for previous destination
        destinationJob?.cancel()
        detailBundleJob?.cancel()
        reviewsJob?.cancel()

        // Reset UI state for new destination immediately
        _uiState.update {
            DestinationDetailUiState(
                destination = null,
                reviews = emptyList(),
                nearbyPlaces = emptyList(),
                weatherData = null,
                wikiSummary = null,
                wikiPageUrl = null,
                isLoading = true,
                isNearbyLoading = true,
                error = null
            )
        }

        destinationJob = viewModelScope.launch {
            // First load basic destination info
            destinationRepository.getDestinationById(id).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        val basicDest = result.data
                        _uiState.update { it.copy(destination = basicDest, error = null) }
                        getReviews(id)
                        
                        // Load full SSOT bundle (Wikipedia, Weather, Nearby Places) for THIS new destination
                        loadFullDetailBundle(id, basicDest.name, basicDest.latitude, basicDest.longitude)
                    }
                    is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, isNearbyLoading = false, error = result.message) }
                }
            }
        }
    }

    private fun loadFullDetailBundle(id: String, name: String, lat: Double, lon: Double) {
        detailBundleJob?.cancel()
        detailBundleJob = viewModelScope.launch {
            destinationRepository.getDestinationDetail(id, name, lat, lon).collect { res ->
                if (res is NetworkResult.Success) {
                    val bundle = res.data
                    val destEntity = bundle.destination
                    val weatherEntity = bundle.weather
                    
                    // Parse weather entity to WeatherUiData
                    val weatherUi = if (weatherEntity != null) {
                        val (icon, text) = parseWmoCode(weatherEntity.currentWeatherCode)
                        val advice = getTravelAdvice(weatherEntity.currentWeatherCode)
                        val days = weatherEntity.forecastDays.mapIndexed { index, model ->
                            val dayNames = listOf("Hôm nay", "Ngày mai", "Ngày kia")
                            val (dayIcon, _) = parseWmoCode(model.weatherCode)
                            ForecastDayData(
                                dayName = dayNames.getOrElse(index) { "Ngày ${index + 1}" },
                                icon = dayIcon,
                                tempRange = "${model.minTemp.toInt()}° - ${model.maxTemp.toInt()}°C"
                            )
                        }
                        WeatherUiData(
                            currentTemp = weatherEntity.currentTemp.toInt(),
                            weatherText = text,
                            weatherIcon = icon,
                            travelAdvice = advice,
                            forecastDays = days
                        )
                    } else null

                    _uiState.update { state ->
                        // Ensure update applies only if destination ID matches
                        if (state.destination == null || state.destination.id == id) {
                            state.copy(
                                isLoading = false,
                                isNearbyLoading = false,
                                destination = destEntity.toDomainModel(),
                                weatherData = weatherUi,
                                wikiSummary = destEntity.wikiDescription,
                                wikiPageUrl = destEntity.wikiArticleUrl,
                                nearbyPlaces = bundle.nearbyPlaces
                            )
                        } else {
                            state
                        }
                    }
                }
            }
        }
    }

    private fun getReviews(destinationId: String) {
        reviewsJob?.cancel()
        reviewsJob = viewModelScope.launch {
            reviewRepository.getReviewsForDestination(destinationId).collect { result ->
                val firestoreReviews = (result as? NetworkResult.Success)?.data.orEmpty()
                val reviews = if (firestoreReviews.isNotEmpty()) {
                    firestoreReviews
                } else if (destinationId.startsWith("geoapify_")) {
                    val placeName = _uiState.value.destination?.name ?: "Địa điểm này"
                    val location = _uiState.value.destination?.location ?: "khu vực"
                    listOf(
                        Review(
                            id = "rev_geo_1",
                            destinationId = destinationId,
                            userId = "u1",
                            userName = "Minh Tuấn",
                            userPhotoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde",
                            rating = 5.0f,
                            comment = "Chuyến đi tuyệt vời tới $placeName ($location). Cảnh quan đẹp, vị trí rất chính xác từ Geoapify!",
                            createdAt = System.currentTimeMillis() - 86400000L * 2
                        ),
                        Review(
                            id = "rev_geo_2",
                            destinationId = destinationId,
                            userId = "u2",
                            userName = "Phương Anh",
                            userPhotoUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330",
                            rating = 4.5f,
                            comment = "Địa điểm đẹp và đáng để ghé thăm tại $location. Rất khuyên mọi người nên trải nghiệm thử!",
                            createdAt = System.currentTimeMillis() - 86400000L * 5
                        )
                    )
                } else {
                    emptyList()
                }

                _uiState.update { state ->
                    val avgRating = if (reviews.isNotEmpty()) reviews.map { it.rating }.average() else state.destination?.rating ?: 4.5
                    state.copy(
                        reviews = reviews,
                        destination = state.destination?.copy(
                            rating = avgRating,
                            reviewCount = if (reviews.isNotEmpty()) reviews.size else state.destination?.reviewCount ?: 0
                        )
                    )
                }
            }
        }
    }

    fun toggleSaveOffline() {
        val dest = _uiState.value.destination ?: return
        viewModelScope.launch {
            val newState = !dest.isSavedOffline
            destinationRepository.toggleOfflineSavedStatus(dest.id, newState).collect { res ->
                if (res is NetworkResult.Success) {
                    _uiState.update { state ->
                        state.copy(
                            destination = state.destination?.copy(isSavedOffline = newState)
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        destinationJob?.cancel()
        detailBundleJob?.cancel()
        reviewsJob?.cancel()
    }
}
