package com.silkfinik.fairsplit.core.domain.usecase.member

import com.silkfinik.fairsplit.core.domain.repository.MemberRepository
import com.silkfinik.fairsplit.core.model.Member
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMembersUseCase @Inject constructor(
    private val memberRepository: MemberRepository
) {
    operator fun invoke(groupId: String): Flow<List<Member>> {
        return memberRepository.getMembers(groupId)
    }
}
