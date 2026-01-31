package com.silkfinik.fairsplit.core.domain.usecase.group

import com.silkfinik.fairsplit.core.model.Expense
import com.silkfinik.fairsplit.core.model.Member
import javax.inject.Inject

class CalculateGroupBalanceUseCase @Inject constructor() {

    operator fun invoke(expenses: List<Expense>, members: List<Member>): Map<String, Double> {
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

        return balances.filterKeys { id ->
            !redirectMap.containsKey(id)
        }
    }
}
