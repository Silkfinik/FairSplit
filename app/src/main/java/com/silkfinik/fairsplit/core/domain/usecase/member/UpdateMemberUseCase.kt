package com.silkfinik.fairsplit.core.domain.usecase.member

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.MemberRepository
import com.silkfinik.fairsplit.core.model.Member
import javax.inject.Inject

class UpdateMemberUseCase @Inject constructor(
    private val memberRepository: MemberRepository
) {
    suspend operator fun invoke(member: Member): Result<Unit> {
        return memberRepository.updateMember(member.copy(updatedAt = System.currentTimeMillis()))
    }
}