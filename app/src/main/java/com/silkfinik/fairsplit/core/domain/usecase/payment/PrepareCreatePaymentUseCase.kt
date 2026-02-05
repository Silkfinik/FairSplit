package com.silkfinik.fairsplit.core.domain.usecase.payment

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.safeCall
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.domain.repository.MemberRepository
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Member
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PrepareCreatePaymentUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
    private val authRepository: AuthRepository
) {
    data class Date(
        val currency: Currency,
        val members: List<Member>,
        val currentUserId: String
    )

    suspend operator fun invoke(groupId: String): Result<Date> = safeCall {
        val group = groupRepository.getGroup(groupId).first()
            ?: throw Exception("Группа не найдена")

        val members = memberRepository.getMembers(groupId).first()

        val currentUserId = authRepository.getUserId()
            ?: throw Exception("Пользователь не авторизован")

        val displayMembers = members.filter { it.mergedWithUid == null }

        Date(
            currency = group.currency,
            members = displayMembers,
            currentUserId = currentUserId
        )
    }
}