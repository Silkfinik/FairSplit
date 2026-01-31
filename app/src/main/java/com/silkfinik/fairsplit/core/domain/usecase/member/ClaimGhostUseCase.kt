package com.silkfinik.fairsplit.core.domain.usecase.member

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.MemberRepository
import javax.inject.Inject

class ClaimGhostUseCase @Inject constructor(
    private val memberRepository: MemberRepository
) {
    suspend operator fun invoke(groupId: String, ghostId: String): Result<Unit> {
        return memberRepository.claimGhost(groupId, ghostId)
    }
}
