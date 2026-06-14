package com.example.travelmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.domain.model.*
import com.example.travelmate.domain.repository.AuthRepository
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
    val members: List<TripMember> = emptyList(),
    val currentUserMember: TripMember? = null,
    val expenses: List<Expense> = emptyList(),
    val totalSpent: Double = 0.0,
    val logs: List<TripLog> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripDetailUiState())
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    private var tripJob: Job? = null
    private var membersJob: Job? = null
    private var expensesJob: Job? = null
    private var logsJob: Job? = null

    fun getTrip(id: String) {
        if (tripJob?.isActive == true) return
        
        tripJob = viewModelScope.launch {
            tripRepository.getTripById(id).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        if (_uiState.value.trip == null) {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update { it.copy(isLoading = false, trip = result.data, error = null) }
                        if (membersJob?.isActive != true) fetchMembers(id)
                        if (expensesJob?.isActive != true) fetchExpenses(id)
                        if (logsJob?.isActive != true) fetchLogs(id)
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            }
        }
    }

    private fun fetchMembers(tripId: String) {
        membersJob = viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            tripRepository.getTripMembers(tripId).collect { result ->
                if (result is NetworkResult.Success) {
                    val currentMember = result.data.find { it.userId == currentUser?.uid }
                    _uiState.update { it.copy(members = result.data, currentUserMember = currentMember) }
                }
            }
        }
    }

    private fun fetchExpenses(tripId: String) {
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

    private fun fetchLogs(tripId: String) {
        logsJob = viewModelScope.launch {
            tripRepository.getTripLogs(tripId).collect { result ->
                if (result is NetworkResult.Success) {
                    _uiState.update { it.copy(logs = result.data) }
                }
            }
        }
    }

    private fun logAction(action: String, details: String) {
        val tripId = _uiState.value.trip?.id ?: return
        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: return@launch
            val log = TripLog(
                tripId = tripId,
                userId = user.uid,
                userName = user.fullName ?: user.displayName ?: "Traveler",
                action = action,
                details = details
            )
            tripRepository.addTripLog(log).collect { }
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

    private fun checkBudgetThreshold(totalSpent: Double) {
        val trip = _uiState.value.trip ?: return
        if (trip.budgetAmount > 0) {
            val percentage = totalSpent / trip.budgetAmount
            if (percentage >= 0.8) {
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

    fun saveActivity(dayIndex: Int, activity: TripActivity, imageBytes: ByteArray? = null) {
        val tripId = _uiState.value.trip?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            var finalActivity = activity
            if (imageBytes != null) {
                val path = "trips/$tripId/activities/${UUID.randomUUID()}.jpg"
                tripRepository.uploadImage(path, imageBytes).collect { result ->
                    if (result is NetworkResult.Success) finalActivity = finalActivity.copy(imageUrl = result.data)
                }
            }
            val flow = if (activity.id.isEmpty()) tripRepository.addActivity(tripId, dayIndex, finalActivity)
                       else tripRepository.updateActivity(tripId, dayIndex, finalActivity)
            flow.collect { result ->
                if (result is NetworkResult.Success) {
                    logAction(if (activity.id.isEmpty()) TripLog.ACTION_ADD_ACTIVITY else TripLog.ACTION_EDIT_ACTIVITY, activity.title)
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun deleteActivity(dayIndex: Int, activityId: String) {
        val trip = _uiState.value.trip ?: return
        val activity = trip.itinerary.getOrNull(dayIndex)?.activities?.find { it.id == activityId }
        viewModelScope.launch {
            tripRepository.deleteActivity(trip.id, dayIndex, activityId).collect { result ->
                if (result is NetworkResult.Success) {
                    logAction(TripLog.ACTION_DELETE_ACTIVITY, activity?.title ?: "Unknown")
                    getTrip(trip.id)
                }
            }
        }
    }

    fun toggleActivityDone(dayIndex: Int, activityId: String) {
        val tripId = _uiState.value.trip?.id ?: return
        viewModelScope.launch {
            tripRepository.toggleActivityDone(tripId, dayIndex, activityId).collect { result ->
                if (result is NetworkResult.Success) getTrip(tripId)
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        val expense = _uiState.value.expenses.find { it.id == expenseId }
        viewModelScope.launch {
            tripRepository.deleteExpense(expenseId).collect { result ->
                if (result is NetworkResult.Success) {
                    logAction(TripLog.ACTION_DELETE_EXPENSE, expense?.description ?: "Unknown")
                }
            }
        }
    }

    fun updateTripCover(imageBytes: ByteArray) {
        val tripId = _uiState.value.trip?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val path = "trips/$tripId/cover_${UUID.randomUUID()}.jpg"
            tripRepository.uploadImage(path, imageBytes).collect { result ->
                if (result is NetworkResult.Success) {
                    val updatedTrip = _uiState.value.trip?.copy(coverImageUrl = result.data) ?: return@collect
                    tripRepository.updateTrip(updatedTrip).collect { updateResult ->
                        if (updateResult is NetworkResult.Success) {
                            logAction(TripLog.ACTION_UPDATE_TRIP, "Đã cập nhật ảnh bìa")
                            _uiState.update { it.copy(isLoading = false, trip = updatedTrip) }
                        }
                    }
                }
            }
        }
    }

    fun updateTrip(trip: Trip, onSuccess: () -> Unit) {
        viewModelScope.launch {
            tripRepository.updateTrip(trip).collect { result ->
                if (result is NetworkResult.Success) {
                    logAction(TripLog.ACTION_UPDATE_TRIP, "Cập nhật thông tin chuyến đi")
                    onSuccess()
                }
            }
        }
    }

    fun canEdit(): Boolean = _uiState.value.currentUserMember?.role.let { it == TripMember.ROLE_OWNER || it == TripMember.ROLE_EDITOR }
    fun isOwner(): Boolean = _uiState.value.currentUserMember?.role == TripMember.ROLE_OWNER

    fun updateMemberRole(userId: String, role: String) {
        val tripId = _uiState.value.trip?.id ?: return
        val member = _uiState.value.members.find { it.userId == userId }
        val tripName = _uiState.value.trip?.name ?: "Chuyến đi"
        
        viewModelScope.launch {
            tripRepository.updateMemberRole(tripId, userId, role).collect { result ->
                if (result is NetworkResult.Success) {
                    logAction(TripLog.ACTION_CHANGE_ROLE, "Thay đổi vai trò ${member?.userName} thành $role")
                    
                    // Send notification to the affected user
                    val notification = Notification(
                        userId = userId,
                        title = "Thay đổi vai trò!",
                        message = "Bạn đã được cập nhật thành $role trong chuyến đi '$tripName'.",
                        type = "role_change",
                        tripId = tripId
                    )
                    notificationRepository.addNotification(notification).collect { }
                }
            }
        }
    }

    fun transferOwnership(newOwnerId: String) {
        val tripId = _uiState.value.trip?.id ?: return
        val currentOwnerId = _uiState.value.currentUserMember?.userId ?: return
        val newOwner = _uiState.value.members.find { it.userId == newOwnerId }
        val tripName = _uiState.value.trip?.name ?: "Chuyến đi"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            tripRepository.transferOwnership(tripId, currentOwnerId, newOwnerId).collect { result ->
                if (result is NetworkResult.Success) {
                    logAction(TripLog.ACTION_CHANGE_ROLE, "Chuyển quyền trưởng nhóm cho ${newOwner?.userName}")
                    
                    // Notify new owner
                    val notification = Notification(
                        userId = newOwnerId,
                        title = "Bạn là trưởng nhóm mới!",
                        message = "Bạn đã được chuyển quyền trưởng nhóm cho chuyến đi '$tripName'.",
                        type = "role_change",
                        tripId = tripId
                    )
                    notificationRepository.addNotification(notification).collect { }

                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun removeMember(userId: String) {
        val tripId = _uiState.value.trip?.id ?: return
        val member = _uiState.value.members.find { it.userId == userId }
        viewModelScope.launch {
            tripRepository.removeMember(tripId, userId).collect { result ->
                if (result is NetworkResult.Success) {
                    logAction(TripLog.ACTION_CHANGE_ROLE, "Đã xóa ${member?.userName} khỏi nhóm")
                }
            }
        }
    }

    fun leaveTrip(onSuccess: () -> Unit) {
        val tripId = _uiState.value.trip?.id ?: return
        val userId = _uiState.value.currentUserMember?.userId ?: return
        viewModelScope.launch {
            tripRepository.leaveTrip(tripId, userId).collect { result ->
                if (result is NetworkResult.Success) {
                    logAction(TripLog.ACTION_LEAVE_TRIP, "Thành viên đã rời nhóm")
                    onSuccess()
                }
            }
        }
    }

    fun addPhotos(byteArrayList: List<ByteArray>) {
        val tripId = _uiState.value.trip?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            byteArrayList.forEach { bytes ->
                tripRepository.uploadImage("gallery/$tripId/${UUID.randomUUID()}.jpg", bytes).collect { result ->
                    if (result is NetworkResult.Success) {
                        tripRepository.addGalleryPhoto(tripId, result.data).collect { }
                    }
                }
            }
            logAction(TripLog.ACTION_UPDATE_TRIP, "Đã thêm ${byteArrayList.size} ảnh vào bộ sưu tập")
            _uiState.update { it.copy(isLoading = false) }
            getTrip(tripId)
        }
    }

    fun deletePhoto(url: String) {
        val tripId = _uiState.value.trip?.id ?: return
        viewModelScope.launch {
            tripRepository.deleteGalleryPhoto(tripId, url).collect { result ->
                if (result is NetworkResult.Success) {
                    logAction(TripLog.ACTION_UPDATE_TRIP, "Đã xóa ảnh khỏi bộ sưu tập")
                    getTrip(tripId)
                }
            }
        }
    }

    fun deleteTrip(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            tripRepository.deleteTrip(id).collect { result ->
                if (result is NetworkResult.Success) onSuccess()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tripJob?.cancel()
        membersJob?.cancel()
        expensesJob?.cancel()
        logsJob?.cancel()
    }
}
