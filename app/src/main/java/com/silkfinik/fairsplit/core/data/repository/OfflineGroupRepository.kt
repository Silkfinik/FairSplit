package com.silkfinik.fairsplit.core.data.repository

import com.silkfinik.fairsplit.core.database.dao.GroupDao
import com.silkfinik.fairsplit.core.database.entity.GroupEntity
import com.silkfinik.fairsplit.core.model.Currency
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class OfflineGroupRepository @Inject constructor(
    private val groupDao: GroupDao
) : GroupRepository {

    override fun getGroups(): Flow<List<GroupEntity>> {
        return groupDao.getGroups()
    }

    override fun getGroup(id: String): Flow<GroupEntity?> {
        return groupDao.getGroup(id)
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
        return newId
    }

    override suspend fun updateGroup(group: GroupEntity) {
        val updatedGroup = group.copy(
            updated_at = System.currentTimeMillis(),
            is_dirty = true
        )
        groupDao.updateGroup(updatedGroup)
    }
}