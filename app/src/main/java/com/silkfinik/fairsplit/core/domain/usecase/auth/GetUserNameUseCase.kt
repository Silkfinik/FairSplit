package com.silkfinik.fairsplit.core.domain.usecase.auth

import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import javax.inject.Inject

class GetUserNameUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): String? {
        return authRepository.getUserName()
    }
}
