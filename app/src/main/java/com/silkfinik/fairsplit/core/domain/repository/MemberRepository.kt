package com.silkfinik.fairsplit.core.domain.repository

import com.silkfinik.fairsplit.core.model.Member
import kotlinx.coroutines.flow.Flow
import com.silkfinik.fairsplit.core.common.util.Result

interface MemberRepository {
    fun getMembers(groupId: String): Flow<List<Member>>
    suspend fun addMember(member: Member)
    suspend fun updateMember(member: Member)
    suspend fun deleteMember(groupId: String, memberId: String)
    suspend fun claimGhost(groupId: String, ghostId: String): Result<Unit>
}
