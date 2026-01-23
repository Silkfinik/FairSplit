package com.silkfinik.fairsplit.core.domain.usecase.auth

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import javax.inject.Inject

class LinkGoogleAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): Result<Unit> {
        val result = authRepository.linkGoogleAccount(idToken)
        return if (result.isSuccess) {
            Result.Success(Unit)
        } else {
            Result.Error(result.exceptionOrNull()?.message ?: "Ошибка привязки Google")
        }
    }
}
