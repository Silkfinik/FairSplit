package com.silkfinik.fairsplit.core.domain.usecase.auth

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.UserRepository
import javax.inject.Inject

class SignInWithEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val updateUserUseCase: UpdateUserUseCase
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        val authResult = authRepository.signInWithEmail(email, password)
        if (authResult is Result.Error) return authResult

        val uid = authRepository.getUserId() ?: return Result.Error("Пользователь не найден")

        if (userRepository.userExists(uid)) {
            return Result.Success(Unit)
        }

        val currentName = authRepository.getUserName() ?: "User"

        return updateUserUseCase(
            name = currentName,
            photoUrl = null,
            email = email
        )
    }
}