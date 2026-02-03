package com.silkfinik.fairsplit.core.domain.usecase.member

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.MemberRepository
import com.silkfinik.fairsplit.core.model.Member
import javax.inject.Inject

class UpdateMemberUseCase @Inject constructor(
    private val memberRepository: MemberRepository
) {
    suspend operator fun invoke(member: Member): Result<Unit> {
        return try {
            memberRepository.updateMember(member.copy(updatedAt = System.currentTimeMillis()))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка при обновлении участника", e)
        }
    }
}
