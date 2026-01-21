package com.silkfinik.fairsplit.core.domain.usecase.auth

import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserIdUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<String?> {
        return authRepository.currentUserId
    }

    suspend fun getUserIdSync(): String? {
        return authRepository.getUserId()
    }
}
