package com.silkfinik.fairsplit.core.domain.usecase.auth

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.UserRepository
import javax.inject.Inject

class HandleGoogleSignInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val updateUserUseCase: UpdateUserUseCase
) {
    suspend operator fun invoke(
        idToken: String,
        email: String,
        displayName: String?,
        photoUrl: String?
    ): Result<Unit> {
        val authResult = if (authRepository.hasSession()) {
            authRepository.linkGoogleAccount(idToken)
        } else {
            authRepository.signInWithGoogle(idToken)
        }

        if (authResult is Result.Error) return authResult

        val uid = authRepository.getUserId() ?: return Result.Error("Пользователь не найден")

        if (userRepository.userExists(uid)) {
            return Result.Success(Unit)
        }

        val nameToUpdate = displayName ?: authRepository.getUserName() ?: "User"

        return updateUserUseCase(
            name = nameToUpdate,
            photoUrl = photoUrl,
            email = email
        )
    }
}