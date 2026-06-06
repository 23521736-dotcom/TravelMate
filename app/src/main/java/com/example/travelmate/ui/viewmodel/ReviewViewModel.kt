package com.example.travelmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.domain.model.Review
import com.example.travelmate.domain.repository.AuthRepository
import com.example.travelmate.domain.repository.ReviewRepository
import com.example.travelmate.domain.repository.TripRepository
import com.example.travelmate.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ReviewUiState(
    val reviews: List<Review> = emptyList(),
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    fun getReviews(destinationId: String) {
        viewModelScope.launch {
            reviewRepository.getReviewsForDestination(destinationId).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, reviews = result.data) }
                    is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun submitReview(destinationId: String, rating: Float, comment: String, images: List<ByteArray>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = authRepository.getCurrentUser() ?: return@launch
            
            val uploadedImageUrls = mutableListOf<String>()
            
            // Upload images first
            images.forEach { bytes ->
                val timestamp = System.currentTimeMillis()
                val randomString = UUID.randomUUID().toString().take(6)
                val fileName = "${timestamp}_$randomString.jpg"
                val path = "reviews/$destinationId/$fileName"
                tripRepository.uploadImage(path, bytes).collect { result ->
                    if (result is NetworkResult.Success) {
                        uploadedImageUrls.add(result.data)
                    }
                }
            }

            val review = Review(
                destinationId = destinationId,
                userId = user.uid,
                userName = user.fullName ?: "Traveler",
                userPhotoUrl = user.photoUrl,
                rating = rating,
                comment = comment,
                imageUrls = uploadedImageUrls
            )

            reviewRepository.addReview(review).collect { result ->
                when (result) {
                    is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    else -> {}
                }
            }
        }
    }

    fun resetState() {
        _uiState.update { ReviewUiState() }
    }
}
