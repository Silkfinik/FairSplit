package com.silkfinik.fairsplit.core.domain.repository

import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Group
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun getGroups(): Flow<List<Group>>
    fun getGroup(id: String): Flow<Group?>

    suspend fun createGroup(name: String, currency: Currency, ownerId: String): String
    suspend fun updateGroup(group: Group)

    fun startSync()
    fun stopSync()
}