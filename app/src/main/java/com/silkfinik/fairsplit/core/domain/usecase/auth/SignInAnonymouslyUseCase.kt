package com.silkfinik.fairsplit.core.domain.usecase.auth

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import javax.inject.Inject

class SignInAnonymouslyUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val updateUserUseCase: UpdateUserUseCase
) {
    suspend operator fun invoke(name: String): Result<Unit> {
        if (!authRepository.hasSession()) {
            val signInResult = authRepository.signInAnonymously()
            if (signInResult is Result.Error) return signInResult
        }

        return updateUserUseCase(name)
    }
}