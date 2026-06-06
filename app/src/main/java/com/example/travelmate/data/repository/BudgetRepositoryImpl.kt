package com.example.travelmate.data.repository

import com.example.travelmate.domain.model.MonthlyBudget
import com.example.travelmate.domain.repository.BudgetRepository
import com.example.travelmate.util.NetworkResult
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase
) : BudgetRepository {

    private val budgetsRef = database.getReference("budgets")

    override fun getMonthlyBudget(userId: String, month: String): Flow<NetworkResult<MonthlyBudget?>> = callbackFlow {
        trySend(NetworkResult.Loading)
        budgetsRef.child(userId).child(month).get().addOnSuccessListener { snapshot ->
            try {
                val budget = snapshot.getValue(MonthlyBudget::class.java)
                trySend(NetworkResult.Success(budget))
            } catch (e: Exception) {
                trySend(NetworkResult.Error("Lỗi định dạng ngân sách"))
            } finally {
                close()
            }
        }.addOnFailureListener { e ->
            trySend(NetworkResult.Error(e.message ?: "Failed to fetch budget"))
            close()
        }
        awaitClose()
    }

    override fun saveMonthlyBudget(budget: MonthlyBudget): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        budgetsRef.child(budget.userId).child(budget.month).setValue(budget).addOnSuccessListener {
            trySend(NetworkResult.Success(Unit))
            close()
        }.addOnFailureListener { e ->
            trySend(NetworkResult.Error(e.message ?: "Failed to save budget"))
            close()
        }
        awaitClose()
    }

    override fun updateNotificationFlag(userId: String, month: String, flag: Boolean): Flow<NetworkResult<Unit>> = callbackFlow {
        budgetsRef.child(userId).child(month).child("hasNotifiedThreshold").setValue(flag)
            .addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }.addOnFailureListener {
                trySend(NetworkResult.Error("Failed to update flag"))
                close()
            }
        awaitClose()
    }
}
