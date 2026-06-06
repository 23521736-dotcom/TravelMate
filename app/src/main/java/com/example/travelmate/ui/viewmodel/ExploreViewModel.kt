package com.example.travelmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.domain.model.Category
import com.example.travelmate.domain.model.Destination
import com.example.travelmate.domain.repository.DestinationRepository
import com.example.travelmate.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExploreUiState(
    val destinations: List<Destination> = emptyList(),
    val filteredDestinations: List<Destination> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val destinationRepository: DestinationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(categories = listOf("All") + Category.values().map { it.displayName }) }
        fetchDestinations()
    }

    private fun fetchDestinations() {
        viewModelScope.launch {
            destinationRepository.getDestinations().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is NetworkResult.Success -> {
                        _uiState.update { it.copy(
                            isLoading = false,
                            destinations = result.data,
                            filteredDestinations = result.data
                        )}
                        applyFilters()
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onCategorySelect(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        applyFilters()
    }

    private fun applyFilters() {
        val query = _uiState.value.searchQuery.lowercase()
        val category = _uiState.value.selectedCategory
        
        val filtered = _uiState.value.destinations.filter { dest ->
            val matchesQuery = dest.name.lowercase().contains(query) || 
                               dest.location.lowercase().contains(query)
            val matchesCategory = category == "All" || 
                                  dest.categories.any { it.displayName == category }
            matchesQuery && matchesCategory
        }
        _uiState.update { it.copy(filteredDestinations = filtered) }
    }
}
