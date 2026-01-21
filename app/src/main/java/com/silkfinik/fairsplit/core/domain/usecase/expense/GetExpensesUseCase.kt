package com.silkfinik.fairsplit.core.domain.usecase.expense

import com.silkfinik.fairsplit.core.domain.repository.ExpenseRepository
import com.silkfinik.fairsplit.core.model.Expense
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExpensesUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(groupId: String): Flow<List<Expense>> {
        return expenseRepository.getExpensesForGroup(groupId)
    }
}
