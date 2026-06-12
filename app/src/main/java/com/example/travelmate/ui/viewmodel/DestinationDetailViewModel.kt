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
                        if (reviewsJob?.isActive != true) {
                            getReviews(id)
                        }
                    }
                    is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    private fun getReviews(destinationId: String) {
        reviewsJob = viewModelScope.launch {
            reviewRepository.getReviewsForDestination(destinationId).collect { result ->
                if (result is NetworkResult.Success) {
                    val reviews = result.data
                    val avgRating = if (reviews.isNotEmpty()) reviews.map { it.rating }.average() else _uiState.value.destination?.rating ?: 0.0
                    
                    _uiState.update { state -> 
                        state.copy(
                            reviews = reviews,
                            destination = state.destination?.copy(
                                rating = avgRating,
                                reviewCount = reviews.size
                            )
                        ) 
                    }
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
