package com.silkfinik.fairsplit.core.domain.usecase.group

import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.ExpenseRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.domain.repository.MemberRepository
import com.silkfinik.fairsplit.core.domain.repository.PaymentRepository
import com.silkfinik.fairsplit.core.model.Expense
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.model.Payment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetGroupDetailsScreenDataUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val paymentRepository: PaymentRepository,
    private val memberRepository: MemberRepository,
    private val authRepository: AuthRepository,
    private val calculateGroupBalanceUseCase: CalculateGroupBalanceUseCase
) {
    data class ScreenData(
        val group: Group?,
        val members: List<Member>,
        val expenses: List<Expense>,
        val payments: List<Payment>,
        val balances: Map<String, Double>,
        val currentUserId: String?
    )

    operator fun invoke(groupId: String): Flow<ScreenData> {
        return combine(
            groupRepository.getGroup(groupId),
            expenseRepository.getExpensesForGroup(groupId),
            paymentRepository.getPayments(groupId),
            memberRepository.getMembers(groupId),
            authRepository.currentUserId
        ) { group, expenses, payments, members, userId ->
            val balances = calculateGroupBalanceUseCase(expenses, members, payments)

            ScreenData(
                group = group,
                members = members,
                expenses = expenses,
                payments = payments,
                balances = balances,
                currentUserId = userId
            )
        }
    }
}