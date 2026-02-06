package com.silkfinik.fairsplit.core.data.repository

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.TimeProvider
import com.silkfinik.fairsplit.core.common.util.safeCall
import com.silkfinik.fairsplit.core.data.datasource.CloudFunctionsDataSource
import com.silkfinik.fairsplit.core.data.mapper.asDomainModel
import com.silkfinik.fairsplit.core.data.mapper.asEntity
import com.silkfinik.fairsplit.core.data.worker.WorkManagerSyncManager
import com.silkfinik.fairsplit.core.database.dao.MemberDao
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.MemberRepository
import com.silkfinik.fairsplit.core.model.Member
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineMemberRepository @Inject constructor(
    private val memberDao: MemberDao,
    private val cloudFunctionsDataSource: CloudFunctionsDataSource,
    private val authRepository: AuthRepository,
    private val workManagerSyncManager: WorkManagerSyncManager,
    private val timeProvider: TimeProvider
) : MemberRepository {

    override fun getMembers(groupId: String): Flow<List<Member>> {
        return memberDao.getMembers(groupId).map { entities ->
            entities.map { it.asDomainModel() }
        }
    }

    override suspend fun addMember(member: Member): Result<Unit> = safeCall {
        memberDao.insertMember(member.asEntity(isDirty = true))
        workManagerSyncManager.scheduleSync()
    }

    override suspend fun updateMember(member: Member): Result<Unit> = safeCall {
        val updatedMember = member.copy(updatedAt = timeProvider.now())
        memberDao.updateMember(updatedMember.asEntity(isDirty = true))
        workManagerSyncManager.scheduleSync()
    }

    override suspend fun deleteMember(groupId: String, memberId: String): Result<Unit> = safeCall {
        val memberEntity = memberDao.getMember(groupId, memberId) ?: throw Exception("User not found")
        memberDao.deleteMember(memberEntity)
        workManagerSyncManager.scheduleSync()
    }

    override suspend fun claimGhost(groupId: String, ghostId: String): Result<Unit> {
        val result = cloudFunctionsDataSource.claimGhost(groupId, ghostId)
        if (result is Result.Success) {
            val userId = authRepository.getUserId()
            val localMember = memberDao.getMember(groupId, ghostId)
            if (userId != null && localMember != null) {
                memberDao.updateMember(localMember.copy(
                    mergedWithUid = userId,
                    isDirty = false,
                    updatedAt = timeProvider.now()
                ))
            }
            workManagerSyncManager.scheduleSync()
        }
        return result
    }
}