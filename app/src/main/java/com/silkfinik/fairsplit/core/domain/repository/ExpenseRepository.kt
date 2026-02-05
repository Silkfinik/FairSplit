package com.silkfinik.fairsplit.core.domain.repository

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.model.Expense
import com.silkfinik.fairsplit.core.model.HistoryItem
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getExpensesForGroup(groupId: String): Flow<List<Expense>>
    fun getExpense(expenseId: String): Flow<Expense?>

    suspend fun createExpense(expense: Expense): Result<String>
    suspend fun updateExpense(expense: Expense): Result<Unit>
    suspend fun deleteExpense(expenseId: String): Result<Unit>

    fun getExpenseHistory(groupId: String, expenseId: String): Flow<List<HistoryItem>>
    fun startSync(groupId: String)
    fun stopSync(groupId: String)
}