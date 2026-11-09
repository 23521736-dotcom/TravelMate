package com.example.travelmate.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.data.local.ReviewDao
import com.example.travelmate.data.local.SyncStatus
import com.example.travelmate.data.model.toEntity
import com.example.travelmate.domain.model.Review
import com.example.travelmate.domain.repository.AuthRepository
import com.example.travelmate.domain.repository.DestinationRepository
import com.example.travelmate.domain.repository.ReviewRepository
import com.example.travelmate.domain.repository.TripRepository
import com.example.travelmate.util.ImageCompressor
import com.example.travelmate.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val tripRepository: TripRepository,
    private val destinationRepository: DestinationRepository,
    private val reviewDao: ReviewDao
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

    fun submitReviewWithUris(
        context: Context,
        destinationId: String,
        rating: Float,
        comment: String,
        imageUris: List<Uri>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = authRepository.getCurrentUser() ?: return@launch

            val destResult = destinationRepository.getDestinationById(destinationId).first()
            if (destResult is NetworkResult.Success) {
                val destinationName = destResult.data.name
                val destinationImage = destResult.data.coverImage ?: ""
                
                val uploadedImageUrls = mutableListOf<String>()
                val localSavedUris = mutableListOf<String>()
                var hasUploadError = false

                // Process images sequentially to keep RAM footprint minimal (<20MB)
                for (uri in imageUris) {
                    val compressedBytes = ImageCompressor.compressImageUri(context, uri, maxDimension = 1024, quality = 80)
                    if (compressedBytes != null) {
                        val timestamp = System.currentTimeMillis()
                        val randomString = UUID.randomUUID().toString().take(6)
                        val fileName = "${timestamp}_$randomString.jpg"
                        val path = "reviews/$destinationId/$fileName"

                        // Save a compressed copy in app's internal storage for offline queue persistence
                        val internalFile = ImageCompressor.saveToInternalStorage(context, compressedBytes, fileName)
                        if (internalFile != null) {
                            localSavedUris.add(Uri.fromFile(internalFile).toString())
                        }

                        // Try online upload
                        try {
                            val uploadResult = tripRepository.uploadImage(path, compressedBytes).first { it !is NetworkResult.Loading }
                            if (uploadResult is NetworkResult.Success) {
                                uploadedImageUrls.add(uploadResult.data)
                            } else {
                                hasUploadError = true
                            }
                        } catch (e: Exception) {
                            hasUploadError = true
                        }
                    }
                }

                val reviewId = UUID.randomUUID().toString()
                val review = Review(
                    id = reviewId,
                    destinationId = destinationId,
                    userId = user.uid,
                    userName = user.fullName ?: "Traveler",
                    userPhotoUrl = user.photoUrl,
                    rating = rating,
                    comment = comment,
                    imageUrls = uploadedImageUrls,
                    destinationName = destinationName,
                    destinationImage = destinationImage
                )

                if (hasUploadError || uploadedImageUrls.size < imageUris.size) {
                    // Offline or partial upload failure -> Save to Room DB with PENDING_CREATE sync status
                    withContext(Dispatchers.IO) {
                        val reviewEntity = review.toEntity(
                            syncStatus = SyncStatus.PENDING_CREATE,
                            localImageUris = localSavedUris
                        )
                        reviewDao.insertReview(reviewEntity)
                    }
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                } else {
                    // All images uploaded online -> Save to Firestore & Room
                    reviewRepository.addReview(review).collect { result ->
                        when (result) {
                            is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                            is NetworkResult.Error -> {
                                // Fallback save locally if Firestore network request fails
                                withContext(Dispatchers.IO) {
                                    reviewDao.insertReview(review.toEntity(SyncStatus.PENDING_CREATE, localSavedUris))
                                }
                                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                            }
                            else -> {}
                        }
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Không tìm thấy thông tin địa danh") }
            }
        }
    }

    fun resetState() {
        _uiState.update { ReviewUiState() }
    }
}
