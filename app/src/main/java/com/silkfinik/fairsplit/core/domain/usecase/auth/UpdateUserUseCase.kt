package com.silkfinik.fairsplit.core.domain.usecase.auth

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.TimeProvider
import com.silkfinik.fairsplit.core.domain.model.AppError
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.UserRepository
import com.silkfinik.fairsplit.core.model.User
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(name: String, photoUrl: String? = null, email: String? = null): Result<Unit> {
        val authResult = authRepository.updateProfile(name, photoUrl)
        if (authResult is Result.Error) {
            return authResult
        }

        val userId = authRepository.getUserId()
            ?: return Result.Error(AppError.Auth.UserNotFound)

        val isActuallyAnonymous = authRepository.isAnonymous()

        val existingUser = userRepository.getUser(userId).first()
        val currentTime = timeProvider.now()

        val user = existingUser?.copy(
            displayName = name,
            photoUrl = photoUrl ?: existingUser.photoUrl,
            email = email ?: existingUser.email,
            isAnonymous = isActuallyAnonymous,
            updatedAt = currentTime
        ) ?: User(
            id = userId,
            displayName = name,
            photoUrl = photoUrl,
            email = email,
            isAnonymous = isActuallyAnonymous,
            createdAt = currentTime,
            updatedAt = currentTime
        )

        return userRepository.createOrUpdateUser(user)
    }
}