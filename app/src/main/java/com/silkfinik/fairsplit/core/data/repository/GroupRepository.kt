package com.silkfinik.fairsplit.core.data.repository

import com.silkfinik.fairsplit.core.database.entity.GroupEntity
import com.silkfinik.fairsplit.core.model.Currency
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun getGroups(): Flow<List<GroupEntity>>
    fun getGroup(id: String): Flow<GroupEntity?>

    suspend fun createGroup(name: String, currency: Currency, ownerId: String): String
    suspend fun updateGroup(group: GroupEntity)
}