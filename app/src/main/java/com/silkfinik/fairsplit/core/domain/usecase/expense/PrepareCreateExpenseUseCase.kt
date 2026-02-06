package com.silkfinik.fairsplit.core.domain.usecase.expense

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.model.AppError
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.ExpenseRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.domain.repository.MemberRepository
import com.silkfinik.fairsplit.core.domain.service.MemberResolutionService
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.model.enums.ExpenseCategory
import com.silkfinik.fairsplit.core.model.enums.SplitType
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PrepareCreateExpenseUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
    private val expenseRepository: ExpenseRepository,
    private val authRepository: AuthRepository,
    private val memberResolutionService: MemberResolutionService
) {
    data class EditorData(
        val currency: Currency,
        val members: List<Member>,
        val currentUserId: String?,
        val isEditing: Boolean = false,
        val isReadOnly: Boolean = false,
        val description: String = "",
        val amount: String = "",
        val category: ExpenseCategory = ExpenseCategory.OTHER,
        val payerId: String? = null,
        val splitType: SplitType = SplitType.EQUAL,
        val splits: Map<String, Double> = emptyMap(),
        val splitData: Map<String, Double> = emptyMap(),
        val selectedMemberIds: Set<String> = emptySet()
    )

    suspend operator fun invoke(groupId: String, expenseId: String?): Result<EditorData> {
        return try {
            val group = groupRepository.getGroup(groupId).first()
                ?: return Result.Error(AppError.Group.NotFound)

            val allMembers = memberRepository.getMembers(groupId).first()
            val currentUserId = authRepository.getUserId()

            val displayMembers = memberResolutionService.getDisplayMembers(allMembers)
            val resolution = memberResolutionService.resolve(allMembers)

            if (expenseId == null) {
                val initialPayerId = displayMembers.firstOrNull { it.id == currentUserId }?.id
                    ?: displayMembers.firstOrNull()?.id

                Result.Success(
                    EditorData(
                        currency = group.currency,
                        members = displayMembers,
                        currentUserId = currentUserId,
                        payerId = initialPayerId,
                        selectedMemberIds = displayMembers.map { it.id }.toSet()
                    )
                )
            } else {
                val expense = expenseRepository.getExpense(expenseId).first()
                    ?: return Result.Error(AppError.Expense.NotFound)

                val originalPayerId = expense.payers.keys.firstOrNull()
                val resolvedPayerId = originalPayerId?.let { resolution.resolveId(it) }

                val resolvedSplits = mutableMapOf<String, Double>()
                expense.splits.forEach { (originalId, amount) ->
                    val targetId = resolution.resolveId(originalId)
                    resolvedSplits[targetId] = (resolvedSplits[targetId] ?: 0.0) + amount
                }

                val resolvedSplitData = mutableMapOf<String, Double>()
                expense.splitData.forEach { (originalId, value) ->
                    val targetId = resolution.resolveId(originalId)
                    resolvedSplitData[targetId] = (resolvedSplitData[targetId] ?: 0.0) + value
                }

                val initialSelected = if (expense.splitType == SplitType.EQUAL) {
                    resolvedSplits.keys
                } else {
                    resolvedSplitData.keys.ifEmpty { resolvedSplits.keys }
                }

                Result.Success(
                    EditorData(
                        currency = group.currency,
                        members = displayMembers,
                        currentUserId = currentUserId,
                        isEditing = true,
                        isReadOnly = expense.creatorId != currentUserId,
                        description = expense.description,
                        amount = expense.amount.toString(),
                        category = ExpenseCategory.fromId(expense.category),
                        payerId = resolvedPayerId,
                        splitType = expense.splitType,
                        splits = resolvedSplits,
                        splitData = resolvedSplitData,
                        selectedMemberIds = initialSelected
                    )
                )
            }
        } catch (e: Exception) {
            Result.Error(AppError.Common.Unknown(e))
        }
    }
}