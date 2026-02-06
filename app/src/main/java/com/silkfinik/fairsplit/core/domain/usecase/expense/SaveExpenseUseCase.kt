package com.silkfinik.fairsplit.core.domain.usecase.expense

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.TimeProvider
import com.silkfinik.fairsplit.core.common.util.map
import com.silkfinik.fairsplit.core.domain.model.AppError
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.ExpenseRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.model.Expense
import com.silkfinik.fairsplit.core.model.enums.SplitType
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class SaveExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository,
    private val timeProvider: TimeProvider
) {
    data class Params(
        val groupId: String,
        val expenseId: String? = null,
        val description: String,
        val amount: Double,
        val category: String,
        val payerId: String,
        val splits: Map<String, Double>,
        val splitType: SplitType,
        val splitData: Map<String, Double>
    )

    suspend operator fun invoke(params: Params): Result<Unit> {
        return try {
            val userId = authRepository.getUserId()
                ?: return Result.Error(AppError.Auth.NotAuthorized)

            val currency = if (params.expenseId == null) {
                val group = groupRepository.getGroup(params.groupId).first()
                    ?: return Result.Error(AppError.Group.NotFound)
                group.currency
            } else {
                null
            }

            val now = timeProvider.now()

            if (params.expenseId != null) {
                val existingExpense = expenseRepository.getExpense(params.expenseId).first()
                    ?: return Result.Error(AppError.Expense.NotFound)

                val updatedExpense = existingExpense.copy(
                    description = params.description,
                    amount = params.amount,
                    category = params.category,
                    payers = mapOf(params.payerId to params.amount),
                    splits = params.splits,
                    splitType = params.splitType,
                    splitData = params.splitData,
                    updatedAt = now
                )
                expenseRepository.updateExpense(updatedExpense)
            } else {
                val expense = Expense(
                    id = UUID.randomUUID().toString(),
                    groupId = params.groupId,
                    description = params.description,
                    amount = params.amount,
                    currency = currency!!,
                    category = params.category,
                    date = now,
                    creatorId = userId,
                    payers = mapOf(params.payerId to params.amount),
                    splits = params.splits,
                    splitType = params.splitType,
                    splitData = params.splitData,
                    createdAt = now,
                    updatedAt = now
                )

                expenseRepository.createExpense(expense).map { Unit }
            }
        } catch (e: Exception) {
            Result.Error(AppError.Common.Unknown(e))
        }
    }
}