package com.example.travelmate.data.repository

import com.example.travelmate.domain.model.MonthlyBudget
import com.example.travelmate.domain.repository.BudgetRepository
import com.example.travelmate.util.NetworkResult
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : BudgetRepository {

    private val budgetsRef = firestore.collection("budgets")

    private fun budgetDoc(userId: String, month: String) =
        budgetsRef.document(userId).collection("months").document(month)

    override fun getMonthlyBudget(userId: String, month: String): Flow<NetworkResult<MonthlyBudget?>> = callbackFlow {
        trySend(NetworkResult.Loading)
        budgetDoc(userId, month).get()
            .addOnSuccessListener { snapshot ->
                try {
                    trySend(NetworkResult.Success(snapshot.toObject(MonthlyBudget::class.java)))
                } catch (e: Exception) {
                    trySend(NetworkResult.Error("Loi dinh dang ngan sach"))
                } finally {
                    close()
                }
            }
            .addOnFailureListener { e ->
                trySend(NetworkResult.Error(e.message ?: "Failed to fetch budget"))
                close()
            }
        awaitClose()
    }

    override fun saveMonthlyBudget(budget: MonthlyBudget): Flow<NetworkResult<Unit>> = callbackFlow {
        trySend(NetworkResult.Loading)
        budgetDoc(budget.userId, budget.month).set(budget)
            .addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(NetworkResult.Error(e.message ?: "Failed to save budget"))
                close()
            }
        awaitClose()
    }

    override fun updateNotificationFlag(userId: String, month: String, flag: Boolean): Flow<NetworkResult<Unit>> = callbackFlow {
        budgetDoc(userId, month).update("hasNotifiedThreshold", flag)
            .addOnSuccessListener {
                trySend(NetworkResult.Success(Unit))
                close()
            }
            .addOnFailureListener {
                trySend(NetworkResult.Error("Failed to update flag"))
                close()
            }
        awaitClose()
    }
}
