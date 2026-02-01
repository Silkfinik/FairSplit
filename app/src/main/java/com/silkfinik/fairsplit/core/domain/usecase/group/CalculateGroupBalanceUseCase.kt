package com.silkfinik.fairsplit.core.domain.usecase.group

import com.silkfinik.fairsplit.core.model.Expense
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.model.Payment
import com.silkfinik.fairsplit.core.model.enums.PaymentStatus
import javax.inject.Inject

class CalculateGroupBalanceUseCase @Inject constructor() {

    operator fun invoke(
        expenses: List<Expense>, 
        members: List<Member>,
        payments: List<Payment> = emptyList()
    ): Map<String, Double> {
        val balances = mutableMapOf<String, Double>()

        val redirectMap = members
            .filter { it.mergedWithUid != null }
            .associate { it.id to it.mergedWithUid!! }

        fun resolveId(id: String): String = redirectMap[id] ?: id

        expenses.forEach { expense ->
            if (expense.isDeleted) return@forEach

            expense.payers.forEach { (originalId, amount) ->
                val targetId = resolveId(originalId)
                balances[targetId] = (balances[targetId] ?: 0.0) + amount
            }

            expense.splits.forEach { (originalId, amount) ->
                val targetId = resolveId(originalId)
                balances[targetId] = (balances[targetId] ?: 0.0) - amount
            }
        }

        payments.forEach { payment ->
            if (payment.status == PaymentStatus.CONFIRMED) {
                val payerTargetId = resolveId(payment.payerId)
                balances[payerTargetId] = (balances[payerTargetId] ?: 0.0) + payment.amount

                val receiverTargetId = resolveId(payment.receiverId)
                balances[receiverTargetId] = (balances[receiverTargetId] ?: 0.0) - payment.amount
            }
        }

        return balances.filterKeys { id ->
            !redirectMap.containsKey(id)
        }
    }
}
