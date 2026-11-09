package com.example.travelmate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY date DESC")
    fun getExpensesForTrip(tripId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE syncStatus != 'SYNCED'")
    fun getPendingSyncExpenses(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertExpenses(expenses: List<ExpenseEntity>)

    @Query("UPDATE expenses SET syncStatus = :status WHERE id = :expenseId")
    fun updateSyncStatus(expenseId: String, status: SyncStatus)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    fun deleteExpense(expenseId: String)
}
