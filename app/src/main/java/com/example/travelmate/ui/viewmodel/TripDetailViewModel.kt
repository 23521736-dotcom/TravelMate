package com.example.travelmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.domain.model.Expense
import com.example.travelmate.domain.model.Notification
import com.example.travelmate.domain.model.Trip
import com.example.travelmate.domain.model.TripActivity
import com.example.travelmate.domain.repository.NotificationRepository
import com.example.travelmate.domain.repository.TripRepository
import com.example.travelmate.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.util.UUID
import javax.inject.Inject

data class TripDetailUiState(
    val trip: Trip? = null,
    val expenses: List<Expense> = emptyList(),
    val totalSpent: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripDetailUiState())
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    private var tripJob: Job? = null
    private var expensesJob: Job? = null

    fun getTrip(id: String) {
        // Tránh tạo nhiều listener trùng lặp
        if (tripJob?.isActive == true) return
        
        android.util.Log.d("TripDetailVM", "Fetching trip details for: $id")
        tripJob = viewModelScope.launch {
            tripRepository.getTripById(id).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        if (_uiState.value.trip == null) {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                    is NetworkResult.Success -> {
                        android.util.Log.d("TripDetailVM", "Successfully fetched trip: ${result.data.name}")
                        _uiState.update { it.copy(isLoading = false, trip = result.data, error = null) }
                        // Chỉ khởi tạo listener chi tiêu MỘT LẦN duy nhất khi chưa có expensesJob
                        if (expensesJob?.isActive != true) {
                            fetchExpenses(id)
                        }
                    }
                    is NetworkResult.Error -> {
                        android.util.Log.e("TripDetailVM", "Error fetching trip: ${result.message}")
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            }
        }
    }

    private fun fetchExpenses(tripId: String) {
        if (expensesJob?.isActive == true) return
        
        expensesJob = viewModelScope.launch {
            tripRepository.getTripExpenses(tripId).collect { result ->
                if (result is NetworkResult.Success) {
                    val totalInVnd = result.data.sumOf { convertToVnd(it.amount, it.currency) }
                    _uiState.update { it.copy(expenses = result.data, totalSpent = totalInVnd) }
                    checkBudgetThreshold(totalInVnd)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tripJob?.cancel()
        expensesJob?.cancel()
    }

    private fun checkBudgetThreshold(totalSpent: Double) {
        val trip = _uiState.value.trip ?: return
        if (trip.budgetAmount > 0) {
            val percentage = totalSpent / trip.budgetAmount
            if (percentage >= 0.8) {
                // Gửi thông báo
                val notification = Notification(
                    userId = trip.userId,
                    title = "Cảnh báo ngân sách!",
                    message = "Bạn đã chi tiêu hết ${(percentage * 100).toInt()}% ngân sách của chuyến đi ${trip.name}.",
                    type = "budget"
                )
                viewModelScope.launch {
                    notificationRepository.addNotification(notification).collect { }
                }
            }
        }
    }

    private fun convertToVnd(amount: Double, currency: String): Double {
        val rate = when (currency.uppercase()) {
            "USD" -> 25400.0
            "EUR" -> 27500.0
            "SGD" -> 18800.0
            "THB" -> 700.0
            else -> 1.0
        }
        return amount * rate
    }

    fun saveActivity(
        dayIndex: Int,
        activity: TripActivity,
        imageBytes: ByteArray? = null
    ) {
        val tripId = _uiState.value.trip?.id ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            var finalActivity = activity
            
            if (imageBytes != null) {
                val timestamp = System.currentTimeMillis()
                val randomStr = UUID.randomUUID().toString().take(6)
                val path = "trips/$tripId/activities/${timestamp}_$randomStr.jpg"
                
                tripRepository.uploadImage(path, imageBytes).collect { result ->
                    if (result is NetworkResult.Success) {
                        finalActivity = finalActivity.copy(imageUrl = result.data)
                    }
                }
            }
            
            val flow = if (activity.id.isEmpty()) {
                tripRepository.addActivity(tripId, dayIndex, finalActivity)
            } else {
                tripRepository.updateActivity(tripId, dayIndex, finalActivity)
            }
            
            flow.collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.update { it.copy(isLoading = false, error = null) }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun deleteActivity(dayIndex: Int, activityId: String) {
        val tripId = _uiState.value.trip?.id ?: return
        viewModelScope.launch {
            tripRepository.deleteActivity(tripId, dayIndex, activityId).collect { result ->
                if (result is NetworkResult.Success) {
                    getTrip(tripId)
                }
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            tripRepository.deleteExpense(expenseId).collect { }
        }
    }

    fun toggleActivityDone(dayIndex: Int, activityId: String) {
        val tripId = _uiState.value.trip?.id ?: return
        viewModelScope.launch {
            tripRepository.toggleActivityDone(tripId, dayIndex, activityId).collect { result ->
                if (result is NetworkResult.Success) {
                    getTrip(tripId)
                }
            }
        }
    }

    fun addPhoto(byteArray: ByteArray) {
        val tripId = _uiState.value.trip?.id ?: return
        val path = "gallery/$tripId/${UUID.randomUUID()}.jpg"
        
        viewModelScope.launch {
            tripRepository.uploadImage(path, byteArray).collect { result ->
                if (result is NetworkResult.Success) {
                    addPhotoUrl(result.data)
                }
            }
        }
    }

    private fun addPhotoUrl(url: String) {
        val tripId = _uiState.value.trip?.id ?: return
        viewModelScope.launch {
            tripRepository.addGalleryPhoto(tripId, url).collect { result ->
                if (result is NetworkResult.Success) {
                    getTrip(tripId)
                }
            }
        }
    }

    fun deletePhoto(url: String) {
        val tripId = _uiState.value.trip?.id ?: return
        viewModelScope.launch {
            tripRepository.deleteGalleryPhoto(tripId, url).collect { result ->
                if (result is NetworkResult.Success) {
                    getTrip(tripId)
                }
            }
        }
    }

    fun deleteTrip(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            tripRepository.deleteTrip(id).collect { result ->
                if (result is NetworkResult.Success) {
                    onSuccess()
                }
            }
        }
    }

    fun updateTrip(trip: Trip, onSuccess: () -> Unit) {
        viewModelScope.launch {
            tripRepository.updateTrip(trip).collect { result ->
                if (result is NetworkResult.Success) {
                    onSuccess()
                }
            }
        }
    }
}
