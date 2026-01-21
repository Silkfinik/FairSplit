package com.silkfinik.fairsplit.core.domain.usecase.expense

import com.silkfinik.fairsplit.core.domain.repository.ExpenseRepository
import com.silkfinik.fairsplit.core.model.Expense
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(expenseId: String): Flow<Expense?> {
        return expenseRepository.getExpense(expenseId)
    }
}
