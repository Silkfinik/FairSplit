package com.silkfinik.fairsplit.core.domain.usecase.expense

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.ExpenseRepository
import com.silkfinik.fairsplit.core.model.HistoryItem
import javax.inject.Inject

class GetExpenseHistoryUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(groupId: String, expenseId: String): Result<List<HistoryItem>> {
        return try {
            val history = repository.getExpenseHistory(groupId, expenseId)
            Result.Success(history)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка загрузки истории")
        }
    }
}
