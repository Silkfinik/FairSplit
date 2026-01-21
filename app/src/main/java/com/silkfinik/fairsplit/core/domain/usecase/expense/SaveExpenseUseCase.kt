package com.silkfinik.fairsplit.core.domain.usecase.expense

import android.util.Log
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.ExpenseRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.model.Expense
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class SaveExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository
) {
    data class Params(
        val groupId: String,
        val expenseId: String? = null,
        val description: String,
        val amount: Double,
        val payerId: String,
        val splitMemberIds: Set<String>
    )

    suspend operator fun invoke(params: Params): Result<Unit> {
        return try {
            val group = groupRepository.getGroup(params.groupId).first() 
                ?: return Result.Error("Группа не найдена")
            val userId = authRepository.getUserId() 
                ?: return Result.Error("Не авторизован")

            // Equal split among selected members
            val splitAmount = params.amount / params.splitMemberIds.size
            val splits = params.splitMemberIds.associateWith { splitAmount }

            if (params.expenseId != null) {
                // Update existing
                val existingExpense = expenseRepository.getExpense(params.expenseId).first() 
                    ?: return Result.Error("Трата не найдена")
                val updatedExpense = existingExpense.copy(
                    description = params.description,
                    amount = params.amount,
                    payers = mapOf(params.payerId to params.amount),
                    splits = splits,
                    updatedAt = System.currentTimeMillis()
                )
                expenseRepository.updateExpense(updatedExpense)
            } else {
                // Create new
                val expense = Expense(
                    id = UUID.randomUUID().toString(),
                    groupId = params.groupId,
                    description = params.description,
                    amount = params.amount,
                    currency = group.currency,
                    date = System.currentTimeMillis(),
                    creatorId = userId,
                    payers = mapOf(params.payerId to params.amount),
                    splits = splits,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                expenseRepository.createExpense(expense)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("SaveExpenseUseCase", "Error saving expense", e)
            Result.Error(e.message ?: "Произошла ошибка при сохранении", e)
        }
    }
}
