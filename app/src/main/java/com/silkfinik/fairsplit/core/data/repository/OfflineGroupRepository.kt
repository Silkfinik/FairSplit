package com.silkfinik.fairsplit.core.data.repository

import com.silkfinik.fairsplit.core.data.mapper.asDomainModel
import com.silkfinik.fairsplit.core.database.dao.GroupDao
import com.silkfinik.fairsplit.core.database.dao.MemberDao
import com.silkfinik.fairsplit.core.database.entity.GroupEntity
import com.silkfinik.fairsplit.core.database.entity.MemberEntity
import com.silkfinik.fairsplit.core.data.sync.GroupRealtimeListener
import com.silkfinik.fairsplit.core.data.worker.WorkManagerSyncManager
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Group
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class OfflineGroupRepository @Inject constructor(
    private val groupDao: GroupDao,
    private val memberDao: MemberDao,
    private val groupRealtimeListener: GroupRealtimeListener,
    private val workManagerSyncManager: WorkManagerSyncManager
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
            owner_id = ownerId,
            created_at = timestamp,
            updated_at = timestamp,
            is_dirty = true
        )

        groupDao.insertGroup(group)

        val member = MemberEntity(
            id = ownerId,
            group_id = newId,
            name = "Я",
            is_ghost = false,
            created_at = timestamp,
            updated_at = timestamp,
            is_dirty = true
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
            updated_at = System.currentTimeMillis(),
            is_dirty = true
        )
        groupDao.updateGroup(updatedGroup)
        workManagerSyncManager.scheduleSync()
    }

    override fun startSync() {
        groupRealtimeListener.startListening()
    }

    override fun stopSync() {
        groupRealtimeListener.stopListening()
    }
}