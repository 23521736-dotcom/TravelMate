package com.example.travelmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.domain.model.Destination
import com.example.travelmate.domain.model.Notification
import com.example.travelmate.domain.model.Trip
import com.example.travelmate.domain.model.User
import com.example.travelmate.domain.repository.AuthRepository
import com.example.travelmate.domain.repository.DestinationRepository
import com.example.travelmate.domain.repository.NotificationRepository
import com.example.travelmate.domain.repository.ReviewRepository
import com.example.travelmate.domain.repository.TripRepository
import com.example.travelmate.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

data class HomeUiState(
    val user: User? = null,
    val ongoingTrip: Trip? = null,
    val totalTrips: Int = 0,
    val totalSpent: Double = 0.0,
    val upcomingTrips: Int = 0,
    val totalReviews: Int = 0,
    val recommendedDestinations: List<Destination> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val destinationRepository: DestinationRepository,
    private val tripRepository: TripRepository,
    private val notificationRepository: NotificationRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var userObservationJob: Job? = null
    private var destinationsJob: Job? = null
    private var tripsJob: Job? = null
    private var expensesJob: Job? = null
    private var notificationsJob: Job? = null
    private var reviewsJob: Job? = null

    init {
        refreshData()
    }

    fun refreshData() {
        observeUserAndRefresh()
        fetchRecommendedDestinations()
    }

    private fun observeUserAndRefresh() {
        userObservationJob?.cancel()
        userObservationJob = viewModelScope.launch {
            authRepository.observeUser().collect { user ->
                _uiState.update { it.copy(user = user) }
                if (user != null) {
                    android.util.Log.d("HomeVM", "User detected: ${user.uid}, fetching user-specific data...")
                    fetchUserTrips(user.uid)
                    fetchUserReviews(user.uid)
                } else {
                    // Reset user-specific data if logged out
                    _uiState.update { it.copy(
                        ongoingTrip = null,
                        totalTrips = 0,
                        totalSpent = 0.0,
                        upcomingTrips = 0,
                        totalReviews = 0
                    ) }
                    tripsJob?.cancel()
                    expensesJob?.cancel()
                    reviewsJob?.cancel()
                    notificationsJob?.cancel()
                }
            }
        }
    }

    private fun fetchRecommendedDestinations() {
        destinationsJob?.cancel()
        destinationsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            destinationRepository.getRecommendedDestinations(6).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        if (_uiState.value.recommendedDestinations.isEmpty()) {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update { it.copy(
                            isLoading = false,
                            recommendedDestinations = result.data
                        )}
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            }
        }
    }

    private fun fetchUserTrips(userId: String) {
        tripsJob?.cancel()
        tripsJob = viewModelScope.launch {
            tripRepository.getUserTrips(userId).collect { result ->
                if (result is NetworkResult.Success) {
                    val trips = result.data
                    val ongoing = trips.find { it.status == Trip.STATUS_ONGOING }
                        ?: trips.filter { it.status == Trip.STATUS_UPCOMING }
                            .minByOrNull { it.startDate }
                    
                    val upcomingCount = trips.count { it.getCalculatedStatus() == Trip.STATUS_UPCOMING }
                    
                    _uiState.update { it.copy(
                        ongoingTrip = ongoing,
                        totalTrips = trips.size,
                        upcomingTrips = upcomingCount
                    ) }
                    
                    checkUpcomingTrips(userId, trips)
                }
            }
        }

        expensesJob?.cancel()
        expensesJob = viewModelScope.launch {
            tripRepository.getAllExpenses(userId).collect { expResult ->
                if (expResult is NetworkResult.Success) {
                    try {
                        val totalSpent = expResult.data.sumOf { exp ->
                            val rate = when (exp.currency.uppercase(java.util.Locale.ROOT)) {
                                "USD" -> 25400.0
                                "EUR" -> 27500.0
                                "SGD" -> 18800.0
                                "THB" -> 700.0
                                else -> 1.0
                            }
                            exp.amount * rate
                        }
                        _uiState.update { it.copy(totalSpent = totalSpent) }
                    } catch (e: Exception) {
                        android.util.Log.e("HomeVM", "Error calculating total spent", e)
                    }
                }
            }
        }
    }

    private fun fetchUserReviews(userId: String) {
        reviewsJob?.cancel()
        reviewsJob = viewModelScope.launch {
            android.util.Log.d("HomeVM", "Starting fetchUserReviews for $userId")
            reviewRepository.getUserReviews(userId).collect { result ->
                if (result is NetworkResult.Success) {
                    android.util.Log.d("HomeVM", "Fetched ${result.data.size} reviews for profile display")
                    _uiState.update { it.copy(totalReviews = result.data.size) }
                } else if (result is NetworkResult.Error) {
                    android.util.Log.e("HomeVM", "Error fetching reviews: ${result.message}")
                }
            }
        }
    }

    private fun checkUpcomingTrips(userId: String, trips: List<Trip>) {
        // We want to handle new trips even if a job was already active, 
        // but we only need one listener for notifications.
        if (notificationsJob?.isActive == true) return
        
        notificationsJob = viewModelScope.launch {
            // Use a separate flow to monitor the trips list to ensure we always use latest
            // However, for simplicity and since checkUpcomingTrips is called on trip updates,
            // we will just collect notifications and check against the current trips in StateFlow.
            
            notificationRepository.getUserNotifications(userId).collect { notifResult ->
                if (notifResult is NetworkResult.Success) {
                    val existingNotifs = notifResult.data
                    val now = System.currentTimeMillis()
                    
                    trips.forEach { trip ->
                        if (trip.getCalculatedStatus() == Trip.STATUS_UPCOMING) {
                            val diff = trip.startDate - now
                            val days = diff / (1000 * 60 * 60 * 24)
                            
                            if (days in 0..3) {
                                val alreadyNotified = existingNotifs.any { 
                                    it.type == "trip_start" && 
                                    it.tripId == trip.id && 
                                    (now - it.timestamp) < (24 * 60 * 60 * 1000) 
                                }
                                
                                if (!alreadyNotified) {
                                    val notification = Notification(
                                        userId = userId,
                                        title = "Chuyến đi sắp tới!",
                                        message = "Chuyến đi ${trip.name} của bạn sẽ bắt đầu trong $days ngày tới.",
                                        type = "trip_start",
                                        tripId = trip.id
                                    )
                                    // Use launch to not block notification collection
                                    launch {
                                        notificationRepository.addNotification(notification).collect { }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userObservationJob?.cancel()
        destinationsJob?.cancel()
        tripsJob?.cancel()
        expensesJob?.cancel()
        notificationsJob?.cancel()
        reviewsJob?.cancel()
    }
}
