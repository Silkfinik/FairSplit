package com.silkfinik.fairsplit.core.domain.repository

import com.silkfinik.fairsplit.core.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUser(uid: String): Flow<User?>
    suspend fun createOrUpdateUser(user: User)
    suspend fun userExists(uid: String): Boolean
}
