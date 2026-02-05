package com.silkfinik.fairsplit.core.domain.usecase.auth

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class LinkEmailAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val updateUserUseCase: UpdateUserUseCase
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        val linkResult = authRepository.linkEmailAccount(email, password)
        if (linkResult is Result.Error) return linkResult

        val userId = authRepository.getUserId()

        val currentUser = userId?.let { userRepository.getUser(it).first() }

        val name = currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: authRepository.getUserName()
            ?: "User"

        return updateUserUseCase(
            name = name,
            email = email
        )
    }
}