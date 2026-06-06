package com.example.travelmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.domain.model.Destination
import com.example.travelmate.domain.model.ItineraryDay
import com.example.travelmate.domain.model.Trip
import com.example.travelmate.domain.repository.AuthRepository
import com.example.travelmate.domain.repository.DestinationRepository
import com.example.travelmate.domain.repository.TripRepository
import com.example.travelmate.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class PlanTripUiState(
    val tripName: String = "",
    val destination: String = "",
    val destinationId: String = "",
    val coverImageUrl: String = "",
    val destinations: List<Destination> = emptyList(),
    val filteredDestinations: List<Destination> = emptyList(),
    val description: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val durationDays: Int = 0,
    val currency: String = "VND",
    val budgetAmount: String = "0",
    val budgetLevel: String = "Mid-range",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val createdTripId: String? = null,
    val error: String? = null
)

@HiltViewModel
class PlanTripViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository,
    private val destinationRepository: DestinationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanTripUiState())
    val uiState: StateFlow<PlanTripUiState> = _uiState.asStateFlow()

    init {
        fetchDestinations()
    }

    private fun fetchDestinations() {
        viewModelScope.launch {
            destinationRepository.getDestinations().collect { result ->
                if (result is NetworkResult.Success) {
                    _uiState.update { it.copy(destinations = result.data) }
                }
            }
        }
    }

    fun onTripNameChange(name: String) = _uiState.update { it.copy(tripName = name) }
    
    fun onDestinationQueryChange(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isEmpty()) emptyList() 
            else state.destinations.filter { it.name.lowercase().contains(query.lowercase()) || it.location.lowercase().contains(query.lowercase()) }
            state.copy(destination = query, filteredDestinations = filtered)
        }
    }

    fun selectDestination(dest: Destination) {
        _uiState.update { it.copy(
            destination = dest.name, 
            destinationId = dest.id,
            coverImageUrl = dest.imageUrls.firstOrNull() ?: "",
            filteredDestinations = emptyList() 
        )}
    }

    fun onDescriptionChange(desc: String) = _uiState.update { it.copy(description = desc) }

    fun onDatesChange(start: String, end: String) {
        val duration = calculateDuration(start, end)
        _uiState.update { it.copy(startDate = start, endDate = end, durationDays = duration) }
    }

    private fun calculateDuration(start: String, end: String): Int {
        if (start.isEmpty() || end.isEmpty()) return 0
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val startDate = sdf.parse(start)
            val endDate = sdf.parse(end)
            if (startDate != null && endDate != null) {
                val diff = endDate.time - startDate.time
                val days = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
                if (days > 0) days else 1
            } else 0
        } catch (e: Exception) {
            0
        }
    }

    fun onCurrencyChange(curr: String) = _uiState.update { it.copy(currency = curr) }
    fun onBudgetChange(amount: String) = _uiState.update { it.copy(budgetAmount = amount) }
    fun onBudgetLevelChange(level: String) = _uiState.update { it.copy(budgetLevel = level) }

    fun getDestinationById(id: String) {
        viewModelScope.launch {
            destinationRepository.getDestinationById(id).collect { result ->
                if (result is NetworkResult.Success) {
                    selectDestination(result.data)
                }
            }
        }
    }

    fun createTrip() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: return@launch
            val state = _uiState.value
            
            if (state.tripName.isEmpty() || state.destination.isEmpty() || state.startDate.isEmpty() || state.endDate.isEmpty()) {
                _uiState.update { it.copy(error = "Vui lòng điền đầy đủ các thông tin có dấu *") }
                return@launch
            }

            val itinerary = mutableListOf<ItineraryDay>()
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            try {
                val startCal = Calendar.getInstance()
                startCal.time = sdf.parse(state.startDate)!!
                
                for (i in 1..state.durationDays) {
                    itinerary.add(ItineraryDay(
                        dayNumber = i,
                        date = startCal.timeInMillis,
                        activities = emptyList()
                    ))
                    startCal.add(Calendar.DAY_OF_MONTH, 1)
                }
            } catch (e: Exception) {}

            val newTrip = Trip(
                userId = user.uid,
                name = state.tripName,
                destination = state.destination,
                destinationId = state.destinationId,
                description = state.description,
                startDate = try { sdf.parse(state.startDate)!!.time } catch(e: Exception) { 0L },
                endDate = try { sdf.parse(state.endDate)!!.time } catch(e: Exception) { 0L },
                durationDays = state.durationDays,
                budgetAmount = state.budgetAmount.toDoubleOrNull() ?: 0.0,
                budgetLevel = state.budgetLevel,
                currency = state.currency,
                coverImageUrl = state.coverImageUrl,
                itinerary = itinerary,
                createdAt = System.currentTimeMillis()
            )

            tripRepository.createTrip(newTrip).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                    is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true, createdTripId = result.data) }
                    is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }
}
