package com.silkfinik.fairsplit.core.domain.repository

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.model.Member
import kotlinx.coroutines.flow.Flow

interface MemberRepository {
    fun getMembers(groupId: String): Flow<List<Member>>

    suspend fun addMember(member: Member): Result<Unit>
    suspend fun updateMember(member: Member): Result<Unit>
    suspend fun deleteMember(groupId: String, memberId: String): Result<Unit>

    suspend fun claimGhost(groupId: String, ghostId: String): Result<Unit>
}