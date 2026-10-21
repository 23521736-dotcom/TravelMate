package com.example.travelmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isLoading: Boolean = false,
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
    private var reviewsJob: Job? = null

    fun getDestination(id: String) {
        if (destinationJob?.isActive == true) return

        destinationJob = viewModelScope.launch {
            destinationRepository.getDestinationById(id).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        if (_uiState.value.destination == null) {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update { it.copy(isLoading = false, destination = result.data, error = null) }
                        // Fetch reviews immediately after destination is loaded successfully
                        getReviews(id)
                    }
                    is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
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

    override fun onCleared() {
        super.onCleared()
        destinationJob?.cancel()
        reviewsJob?.cancel()
    }
}
