package com.example.travelmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.domain.model.Trip
import com.example.travelmate.domain.model.TripMember
import com.example.travelmate.domain.repository.AuthRepository
import com.example.travelmate.domain.repository.TripRepository
import com.example.travelmate.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class TripUiState(
    val trips: List<Trip> = emptyList(),
    val tripsWithRoles: List<Pair<Trip, TripMember?>> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TripViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripUiState())
    val uiState: StateFlow<TripUiState> = _uiState.asStateFlow()

    init {
        refreshTrips()
    }

    fun refreshTrips() {
        if (_uiState.value.isLoading) return
        
        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: return@launch
            tripRepository.getUserTrips(user.uid).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        if (_uiState.value.trips.isEmpty()) {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                    is NetworkResult.Success -> {
                        val trips = result.data
                        // For each trip, fetch the role of the current user
                        val tripsWithRoles = trips.map { trip ->
                            async {
                                val membersResult = tripRepository.getTripMembers(trip.id).first()
                                val role = if (membersResult is NetworkResult.Success) {
                                    membersResult.data.find { it.userId == user.uid }
                                } else null
                                trip to role
                            }
                        }.awaitAll()
                        
                        _uiState.update { it.copy(
                            isLoading = false, 
                            trips = trips, 
                            tripsWithRoles = tripsWithRoles,
                            error = null
                        ) }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            }
        }
    }
}
