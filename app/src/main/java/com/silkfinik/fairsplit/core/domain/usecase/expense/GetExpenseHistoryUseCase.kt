package com.silkfinik.fairsplit.core.domain.usecase.expense

import com.silkfinik.fairsplit.core.domain.repository.ExpenseRepository
import com.silkfinik.fairsplit.core.model.HistoryItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExpenseHistoryUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    operator fun invoke(groupId: String, expenseId: String): Flow<List<HistoryItem>> {
        return repository.getExpenseHistory(groupId, expenseId)
    }
}
