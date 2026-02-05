package com.silkfinik.fairsplit.core.domain.usecase.auth

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.UserRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val userId = authRepository.getUserId() ?: return Result.Error("Пользователь не найден")

        val deleteResult = userRepository.deleteUser(userId)
        if (deleteResult is Result.Error) return deleteResult

        return authRepository.signOut()
    }
}