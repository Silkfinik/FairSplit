package com.silkfinik.fairsplit.core.data.repository

import com.silkfinik.fairsplit.core.data.datasource.CloudFunctionsDataSource
import com.silkfinik.fairsplit.core.data.mapper.asDomainModel
import com.silkfinik.fairsplit.core.data.mapper.asEntity
import com.silkfinik.fairsplit.core.database.dao.MemberDao
import com.silkfinik.fairsplit.core.domain.repository.MemberRepository
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.data.worker.WorkManagerSyncManager
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineMemberRepository @Inject constructor(
    private val memberDao: MemberDao,
    private val cloudFunctionsDataSource: CloudFunctionsDataSource,
    private val authRepository: AuthRepository,
    private val workManagerSyncManager: WorkManagerSyncManager
) : MemberRepository {

    override fun getMembers(groupId: String): Flow<List<Member>> {
        return memberDao.getMembers(groupId).map { entities ->
            entities.map { it.asDomainModel() }
        }
    }

    override suspend fun addMember(member: Member) {
        memberDao.insertMember(member.asEntity(isDirty = true))
        workManagerSyncManager.scheduleSync()
    }

    override suspend fun updateMember(member: Member) {
        memberDao.updateMember(member.asEntity(isDirty = true))
        workManagerSyncManager.scheduleSync()
    }

    override suspend fun deleteMember(groupId: String, memberId: String) {
        val memberEntity = memberDao.getMember(groupId, memberId) ?: return
        memberDao.deleteMember(memberEntity)
        workManagerSyncManager.scheduleSync()
    }

    override suspend fun claimGhost(groupId: String, ghostId: String): Result<Unit> {
        val result = cloudFunctionsDataSource.claimGhost(groupId, ghostId)
        if (result is Result.Success) {
            val userId = authRepository.getUserId()
            val localMember = memberDao.getMember(groupId, ghostId)
            if (userId != null && localMember != null) {
                memberDao.updateMember(localMember.copy(mergedWithUid = userId, isDirty = false))
            }
            workManagerSyncManager.scheduleSync()
        }
        return result
    }
}