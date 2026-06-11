package com.example.travelmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.domain.model.Expense
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
import java.text.SimpleDateFormat
import java.util.*

data class AddExpenseUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    fun addExpense(tripId: String?, amount: String, currency: String, category: String, description: String, dateStr: String) {
        val amountVal = amount.toDoubleOrNull()
        if (amountVal == null || amountVal <= 0) {
            _uiState.update { it.copy(error = "Số tiền phải là số lớn hơn 0") }
            return
        }
        
        if (category.isEmpty()) {
            _uiState.update { it.copy(error = "Vui lòng chọn hạng mục") }
            return
        }

        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.uid ?: ""
            if (userId.isEmpty()) {
                _uiState.update { it.copy(error = "Chưa đăng nhập") }
                return@launch
            }

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val dateMillis = try { 
                sdf.parse(dateStr)?.time ?: System.currentTimeMillis() 
            } catch(e: Exception) { 
                System.currentTimeMillis() 
            }

            val expense = Expense(
                userId = userId,
                tripId = tripId ?: "global",
                amount = amountVal,
                currency = currency,
                category = category,
                description = description,
                date = dateMillis
            )

            tripRepository.addExpense(expense).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }
}
