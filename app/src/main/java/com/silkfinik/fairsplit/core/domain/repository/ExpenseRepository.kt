package com.silkfinik.fairsplit.core.domain.repository

import com.silkfinik.fairsplit.core.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getExpensesForGroup(groupId: String): Flow<List<Expense>>
    fun getExpense(expenseId: String): Flow<Expense?>
    suspend fun createExpense(expense: Expense): String
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expenseId: String)
    fun startSync(groupId: String)
    fun stopSync(groupId: String)
}
