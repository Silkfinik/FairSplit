package com.silkfinik.fairsplit.core.domain.usecase.expense

import com.silkfinik.fairsplit.core.domain.repository.ExpenseRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.domain.repository.MemberRepository
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.HistoryItem
import com.silkfinik.fairsplit.core.model.Member
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class GetExpenseHistoryScreenDataUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val memberRepository: MemberRepository,
    private val groupRepository: GroupRepository
) {
    data class ScreenData(
        val history: List<HistoryItem>,
        val members: Map<String, Member>,
        val currency: Currency?
    )

    operator fun invoke(groupId: String, expenseId: String): Flow<ScreenData> {
        return combine(
            expenseRepository.getExpenseHistory(groupId, expenseId),
            memberRepository.getMembers(groupId),
            groupRepository.getGroup(groupId)
        ) { history, members, group ->
            ScreenData(
                history = history,
                members = members.associateBy { it.id },
                currency = group?.currency
            )
        }
    }
}