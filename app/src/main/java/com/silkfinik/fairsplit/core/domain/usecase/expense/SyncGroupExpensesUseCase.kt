package com.silkfinik.fairsplit.core.domain.usecase.expense

import com.silkfinik.fairsplit.core.domain.repository.ExpenseRepository
import javax.inject.Inject

class SyncGroupExpensesUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    fun start(groupId: String) {
        expenseRepository.startSync(groupId)
    }

    fun stop(groupId: String) {
        expenseRepository.stopSync(groupId)
    }
}
