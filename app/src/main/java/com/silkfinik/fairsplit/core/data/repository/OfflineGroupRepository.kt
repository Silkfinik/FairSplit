package com.silkfinik.fairsplit.core.data.repository

import com.silkfinik.fairsplit.core.data.datasource.CloudFunctionsDataSource
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.asSafeMap
import com.silkfinik.fairsplit.core.data.mapper.asDomainModel
import com.silkfinik.fairsplit.core.database.dao.GroupDao
import com.silkfinik.fairsplit.core.database.dao.MemberDao
import com.silkfinik.fairsplit.core.database.entity.GroupEntity
import com.silkfinik.fairsplit.core.database.entity.MemberEntity
import com.silkfinik.fairsplit.core.data.sync.GroupRealtimeListener
import com.silkfinik.fairsplit.core.data.worker.WorkManagerSyncManager
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Group
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class OfflineGroupRepository @Inject constructor(
    private val groupDao: GroupDao,
    private val memberDao: MemberDao,
    private val groupRealtimeListener: GroupRealtimeListener,
    private val workManagerSyncManager: WorkManagerSyncManager,
    private val authRepository: AuthRepository,
    private val cloudFunctionsDataSource: CloudFunctionsDataSource
) : GroupRepository {

    override fun getGroups(): Flow<List<Group>> {
        return groupDao.getGroups().map { entities ->
            entities.map { it.asDomainModel() }
        }
    }

    override fun getGroup(id: String): Flow<Group?> {
        return groupDao.getGroup(id).map { it?.asDomainModel() }
    }

    override suspend fun createGroup(name: String, currency: Currency, ownerId: String): String {
        val newId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val group = GroupEntity(
            id = newId,
            name = name,
            currency = currency,
            ownerId = ownerId,
            createdAt = timestamp,
            updatedAt = timestamp,
            isDirty = true
        )

        groupDao.insertGroup(group)

        val member = MemberEntity(
            id = ownerId,
            groupId = newId,
            name = authRepository.getUserName() ?: "Я",
            isGhost = false,
            createdAt = timestamp,
            updatedAt = timestamp,
            isDirty = true
        )
        memberDao.insertMember(member)

        workManagerSyncManager.scheduleSync()
        return newId
    }

    override suspend fun updateGroup(group: Group) {
        val existingEntity = groupDao.getGroupById(group.id) ?: return
        
        val updatedGroup = existingEntity.copy(
            name = group.name,
            currency = group.currency,
            updatedAt = System.currentTimeMillis(),
            isDirty = true
        )
        groupDao.updateGroup(updatedGroup)
        workManagerSyncManager.scheduleSync()
    }
    
    override suspend fun joinGroup(code: String): Result<String> {
        val result = cloudFunctionsDataSource.joinByInviteCode(code)
        if (result is Result.Success) {
            workManagerSyncManager.scheduleSync()
        }
        return result
    }

    override suspend fun generateInviteCode(groupId: String): Result<String> {
        return cloudFunctionsDataSource.createInviteCode(groupId)
    }

    override fun startSync() {
        groupRealtimeListener.startListening()
    }

    override fun stopSync() {
        groupRealtimeListener.stopListening()
    }
}