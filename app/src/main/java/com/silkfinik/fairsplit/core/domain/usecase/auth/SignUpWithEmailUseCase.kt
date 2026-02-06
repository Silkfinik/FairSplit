package com.silkfinik.fairsplit.core.domain.usecase.auth

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import javax.inject.Inject

class SignUpWithEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val updateUserUseCase: UpdateUserUseCase
) {
    suspend operator fun invoke(name: String, email: String, password: String): Result<Unit> {
        val authResult = authRepository.signUpWithEmail(name, email, password)
        if (authResult is Result.Error) return authResult

        val updateResult = updateUserUseCase(
            name = name,
            photoUrl = null,
            email = email
        )

        if (updateResult is Result.Error) {
            return Result.Error("Ошибка создания профиля: ${updateResult.message}", updateResult.exception)
        }

        return authRepository.sendEmailVerification()
    }
}