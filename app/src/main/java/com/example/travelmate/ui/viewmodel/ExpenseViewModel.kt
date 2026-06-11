package com.example.travelmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.domain.model.Expense
import com.example.travelmate.domain.model.MonthlyBudget
import com.example.travelmate.domain.model.Notification
import com.example.travelmate.domain.repository.AuthRepository
import com.example.travelmate.domain.repository.BudgetRepository
import com.example.travelmate.domain.repository.NotificationRepository
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

data class ExpenseUiState(
    val isLoading: Boolean = false,
    val expenses: List<Expense> = emptyList(),
    val currentMonthExpenses: List<Expense> = emptyList(),
    val totalSpentVnd: Double = 0.0,
    val currentMonthTotalVnd: Double = 0.0,
    val monthlyBudget: MonthlyBudget? = null,
    val error: String? = null
)

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository,
    private val budgetRepository: BudgetRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    private val currentMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.uid ?: ""
            if (userId.isEmpty()) return@launch

            // Fetch Budget and Expenses concurrently using launch blocks
            launch {
                budgetRepository.getMonthlyBudget(userId, currentMonth).collect { result ->
                    if (result is NetworkResult.Success) {
                        _uiState.update { it.copy(monthlyBudget = result.data) }
                    }
                }
            }

            launch {
                tripRepository.getAllExpenses(userId).collect { result ->
                    when (result) {
                        is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                        is NetworkResult.Success -> {
                            val allExpenses = result.data
                            val total = allExpenses.sumOf { convertToVnd(it.amount, it.currency) }
                            
                            val monthExpenses = allExpenses.filter { 
                                try {
                                    SimpleDateFormat("yyyy-MM", Locale.US).format(Date(it.date)) == currentMonth
                                } catch (e: Exception) { false }
                            }
                            val monthTotal = monthExpenses.sumOf { convertToVnd(it.amount, it.currency) }

                            _uiState.update { it.copy(
                                isLoading = false, 
                                expenses = allExpenses, 
                                currentMonthExpenses = monthExpenses,
                                totalSpentVnd = total,
                                currentMonthTotalVnd = monthTotal
                            ) }

                            checkBudgetThreshold(userId, monthTotal)
                        }
                        is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            }
        }
    }

    private var isNotifying = false

    private fun checkBudgetThreshold(userId: String, currentSpent: Double) {
        val budget = _uiState.value.monthlyBudget ?: return
        if (budget.amount <= 0 || budget.hasNotifiedThreshold || isNotifying) return

        val thresholdAmount = budget.amount * (budget.thresholdPercent / 100.0)
        if (currentSpent >= thresholdAmount) {
            isNotifying = true
            viewModelScope.launch {
                // Add Notification
                val notification = Notification(
                    userId = userId,
                    title = "Cảnh báo ngân sách",
                    message = "Bạn đã chi tiêu vượt quá ${budget.thresholdPercent}% ngân sách tháng $currentMonth.",
                    type = "budget"
                )
                notificationRepository.addNotification(notification).collect { result ->
                    if (result is NetworkResult.Success) {
                        // Mark as notified so we don't spam
                        budgetRepository.updateNotificationFlag(userId, currentMonth, true).collect { flagResult ->
                            if (flagResult is NetworkResult.Success) {
                                _uiState.update { it.copy(monthlyBudget = budget.copy(hasNotifiedThreshold = true)) }
                            }
                            isNotifying = false
                        }
                    } else {
                        isNotifying = false
                    }
                }
            }
        }
    }

    fun saveBudget(amount: Double, thresholdPercent: Int) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.uid ?: ""
            if (userId.isEmpty()) return@launch

            val newBudget = MonthlyBudget(
                userId = userId,
                month = currentMonth,
                amount = amount,
                thresholdPercent = thresholdPercent,
                hasNotifiedThreshold = false
            )

            budgetRepository.saveMonthlyBudget(newBudget).collect { result ->
                if (result is NetworkResult.Success) {
                    _uiState.update { it.copy(monthlyBudget = newBudget) }
                    // Re-check threshold with new budget
                    checkBudgetThreshold(userId, _uiState.value.currentMonthTotalVnd)
                } else if (result is NetworkResult.Error) {
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    fun convertToVnd(amount: Double, currency: String?): Double {
        val safeCurrency = currency?.uppercase() ?: "VND"
        val rate = when (safeCurrency) {
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
