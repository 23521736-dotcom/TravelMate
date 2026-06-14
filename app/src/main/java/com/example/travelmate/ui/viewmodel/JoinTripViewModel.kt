package com.example.travelmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.domain.repository.AuthRepository
import com.example.travelmate.domain.repository.TripRepository
import com.example.travelmate.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JoinTripUiState(
    val inviteCode: String = "",
    val isLoading: Boolean = false,
    val joinedTripId: String? = null,
    val error: String? = null
)

@HiltViewModel
class JoinTripViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinTripUiState())
    val uiState: StateFlow<JoinTripUiState> = _uiState.asStateFlow()

    fun onInviteCodeChange(code: String) {
        _uiState.update { it.copy(inviteCode = code, error = null) }
    }

    fun joinByCode() {
        val code = _uiState.value.inviteCode.trim().uppercase()
        if (code.isEmpty()) {
            _uiState.update { it.copy(error = "Vui lòng nhập mã mời") }
            return
        }
        
        // Removed the strict "GO-" prefix check to support varied code formats (e.g. HL7YYB)
        // while still maintaining basic length and character validation if needed.
        if (code.length < 4) {
            _uiState.update { it.copy(error = "Mã mời quá ngắn") }
            return
        }

        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser() ?: throw Exception("Chưa đăng nhập")
                tripRepository.joinTripByInviteCode(
                    code, 
                    user.uid, 
                    user.fullName ?: user.displayName ?: "Traveler", 
                    user.photoUrl
                ).collect { result ->
                    when (result) {
                        is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                        is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, joinedTripId = result.data) }
                        is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Lỗi không xác định") }
            }
        }
    }

    fun joinByToken(token: String) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: return@launch
            tripRepository.joinTripByShareToken(
                token, 
                user.uid, 
                user.fullName ?: user.displayName ?: "Traveler", 
                user.photoUrl
            ).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                    is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, joinedTripId = result.data) }
                    is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }
}
