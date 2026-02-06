package com.silkfinik.fairsplit.core.domain.usecase.group

import com.silkfinik.fairsplit.core.domain.service.MemberResolutionService
import com.silkfinik.fairsplit.core.model.Expense
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.model.Payment
import com.silkfinik.fairsplit.core.model.enums.PaymentStatus
import javax.inject.Inject

class CalculateGroupBalanceUseCase @Inject constructor(
    private val memberResolutionService: MemberResolutionService
) {

    operator fun invoke(
        expenses: List<Expense>,
        members: List<Member>,
        payments: List<Payment> = emptyList()
    ): Map<String, Double> {
        val balances = mutableMapOf<String, Double>()
        val resolution = memberResolutionService.resolve(members)

        expenses.forEach { expense ->
            if (expense.isDeleted) return@forEach

            expense.payers.forEach { (originalId, amount) ->
                val targetId = resolution.resolveId(originalId)
                balances[targetId] = (balances[targetId] ?: 0.0) + amount
            }

            expense.splits.forEach { (originalId, amount) ->
                val targetId = resolution.resolveId(originalId)
                balances[targetId] = (balances[targetId] ?: 0.0) - amount
            }
        }

        payments.forEach { payment ->
            if (payment.status == PaymentStatus.CONFIRMED) {
                val payerTargetId = resolution.resolveId(payment.payerId)
                balances[payerTargetId] = (balances[payerTargetId] ?: 0.0) + payment.amount

                val receiverTargetId = resolution.resolveId(payment.receiverId)
                balances[receiverTargetId] = (balances[receiverTargetId] ?: 0.0) - payment.amount
            }
        }

        val ghostIds = members.filter { it.mergedWithUid != null }.map { it.id }.toSet()
        return balances.filterKeys { id -> !ghostIds.contains(id) }
    }
}