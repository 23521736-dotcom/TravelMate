package com.example.travelmate.domain.repository

import com.example.travelmate.domain.model.MonthlyBudget
import com.example.travelmate.util.NetworkResult
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getMonthlyBudget(userId: String, month: String): Flow<NetworkResult<MonthlyBudget?>>
    fun saveMonthlyBudget(budget: MonthlyBudget): Flow<NetworkResult<Unit>>
    fun updateNotificationFlag(userId: String, month: String, flag: Boolean): Flow<NetworkResult<Unit>>
}
