package com.silkfinik.fairsplit.core.domain.usecase.auth

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import javax.inject.Inject

class LinkEmailAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val updateUserUseCase: UpdateUserUseCase
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        val linkResult = authRepository.linkEmailAccount(email, password)
        if (linkResult is Result.Error) return linkResult

        val name = authRepository.getUserName() ?: "User"

        return updateUserUseCase(
            name = name,
            email = email
        )
    }
}