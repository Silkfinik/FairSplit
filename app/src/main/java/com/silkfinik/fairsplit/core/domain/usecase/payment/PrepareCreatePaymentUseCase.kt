package com.silkfinik.fairsplit.core.domain.usecase.payment

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.model.AppError
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

    suspend operator fun invoke(groupId: String): Result<Date> {
        return try {
            val group = groupRepository.getGroup(groupId).first()
                ?: return Result.Error(AppError.Group.NotFound)

            val currentUserId = authRepository.getUserId()
                ?: return Result.Error(AppError.Auth.NotAuthorized)

            val members = memberRepository.getMembers(groupId).first()
            val displayMembers = members.filter { it.mergedWithUid == null }

            Result.Success(
                Date(
                    currency = group.currency,
                    members = displayMembers,
                    currentUserId = currentUserId
                )
            )
        } catch (e: Exception) {
            Result.Error(AppError.General(e.message ?: "Ошибка подготовки данных", e))
        }
    }
}