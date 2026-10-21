package com.example.travelmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.domain.model.Category
import com.example.travelmate.domain.model.Destination
import com.example.travelmate.domain.repository.DestinationRepository
import com.example.travelmate.util.NetworkResult
import com.example.travelmate.util.normalizeVietnamese
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.travelmate.domain.repository.GeoapifyRepository
import kotlinx.coroutines.Job
import javax.inject.Inject

enum class SearchMode {
    ALL, FIRESTORE_ONLY, GEOAPIFY_ONLINE
}

data class ExploreUiState(
    val destinations: List<Destination> = emptyList(),
    val filteredDestinations: List<Destination> = emptyList(),
    val geoapifyDestinations: List<Destination> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val searchMode: SearchMode = SearchMode.ALL,
    val isGeoapifyLoading: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val destinationRepository: DestinationRepository,
    private val geoapifyRepository: GeoapifyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var geoapifyJob: Job? = null

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
        searchGeoapify(query)
    }

    fun onSearchModeSelect(mode: SearchMode) {
        _uiState.update { it.copy(searchMode = mode) }
    }

    private fun searchGeoapify(query: String) {
        geoapifyJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _uiState.update { it.copy(geoapifyDestinations = emptyList(), isGeoapifyLoading = false) }
            return
        }

        geoapifyJob = viewModelScope.launch {
            geoapifyRepository.searchPlacesOnline(trimmed).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.update { it.copy(isGeoapifyLoading = true) }
                    is NetworkResult.Success -> _uiState.update { it.copy(isGeoapifyLoading = false, geoapifyDestinations = result.data) }
                    is NetworkResult.Error -> _uiState.update { it.copy(isGeoapifyLoading = false) }
                }
            }
        }
    }

    fun onCategorySelect(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        applyFilters()
    }

    private fun applyFilters() {
        val query = _uiState.value.searchQuery.normalizeVietnamese()
        val category = _uiState.value.selectedCategory
        
        val filtered = _uiState.value.destinations.filter { dest ->
            val matchesQuery = dest.name.normalizeVietnamese().contains(query) || 
                               dest.location.normalizeVietnamese().contains(query)
            val matchesCategory = category == "All" || 
                                  dest.categories.any { it.displayName == category }
            matchesQuery && matchesCategory
        }
        _uiState.update { it.copy(filteredDestinations = filtered) }
    }
}
