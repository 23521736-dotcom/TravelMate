package com.example.travelmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.domain.model.Expense
import com.example.travelmate.domain.repository.AuthRepository
import com.example.travelmate.domain.repository.BudgetRepository
import com.example.travelmate.domain.repository.TripRepository
import com.example.travelmate.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val authRepository: AuthRepository,
    private val budgetRepository: BudgetRepository
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
            _uiState.update { it.copy(isLoading = true, error = null) }
            val userId = authRepository.getCurrentUser()?.uid ?: ""
            if (userId.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, error = "Chưa đăng nhập") }
                return@launch
            }

            // Budget validation for personal expenses
            if (tripId == null || tripId == "global") {
                val currentMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
                
                // 1. Get Monthly Budget
                val budgetResult = budgetRepository.getMonthlyBudget(userId, currentMonth).first()
                val budget = (budgetResult as? NetworkResult.Success)?.data

                if (budget != null && budget.amount > 0) {
                    // 2. Get All Expenses to calculate monthly total
                    val expensesResult = tripRepository.getAllExpenses(userId).first()
                    val allExpenses = (expensesResult as? NetworkResult.Success)?.data ?: emptyList()
                    
                    val monthTotal = allExpenses.filter { 
                        try {
                            SimpleDateFormat("yyyy-MM", Locale.US).format(Date(it.date)) == currentMonth
                        } catch (e: Exception) { false }
                    }.sumOf { convertToVnd(it.amount, it.currency) }

                    if (monthTotal >= budget.amount) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                error = "Đã vượt quá ngân sách tháng! Vui lòng tăng ngân sách trong mục Cài đặt để tiếp tục."
                            ) 
                        }
                        return@launch
                    }

                    val newExpenseVnd = convertToVnd(amountVal, currency)

                    if (monthTotal + newExpenseVnd > budget.amount) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                error = "Chi tiêu này sẽ vượt quá ngân sách còn lại (${String.format(Locale.US, "%,.0f", budget.amount - monthTotal)} đ). Vui lòng tăng ngân sách."
                            ) 
                        }
                        return@launch
                    }
                }
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

    private fun convertToVnd(amount: Double, currency: String): Double {
        val rate = when (currency.uppercase()) {
            "USD" -> 25400.0
            "EUR" -> 27500.0
            "SGD" -> 18800.0
            "THB" -> 750.0
            "JPY" -> 165.0
            "KRW" -> 18.5
            "CNY" -> 3500.0
            "GBP" -> 32000.0
            else -> 1.0
        }
        return amount * rate
    }
}
