package com.silkfinik.fairsplit.core.domain.repository

import com.silkfinik.fairsplit.core.model.Member
import kotlinx.coroutines.flow.Flow

interface MemberRepository {
    fun getMembers(groupId: String): Flow<List<Member>>
    suspend fun addMember(member: Member)
    suspend fun updateMember(member: Member)
    suspend fun deleteMember(groupId: String, memberId: String)
}
