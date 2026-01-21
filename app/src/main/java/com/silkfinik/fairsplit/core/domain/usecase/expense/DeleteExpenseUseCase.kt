package com.silkfinik.fairsplit.core.domain.usecase.expense

import android.util.Log
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.ExpenseRepository
import javax.inject.Inject

class DeleteExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(expenseId: String): Result<Unit> {
        return try {
            expenseRepository.deleteExpense(expenseId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("DeleteExpenseUseCase", "Error deleting expense", e)
            Result.Error(e.message ?: "Ошибка при удалении траты", e)
        }
    }
}
