package com.silkfinik.fairsplit.core.domain.usecase.auth

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.UserRepository
import com.silkfinik.fairsplit.core.model.User
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(name: String, photoUrl: String? = null, email: String? = null): Result<Unit> {
        return try {
            val authResult = authRepository.updateProfile(name, photoUrl)
            if (authResult is Result.Error) {
                return authResult
            }

            val userId = authRepository.getUserId() ?: return Result.Error("Пользователь не найден")
            
            // Check if user exists in DB to preserve createdAt
            val existingUser = userRepository.getUser(userId).first()
            
            val currentTime = System.currentTimeMillis()
            val user = if (existingUser != null) {
                existingUser.copy(
                    displayName = name,
                    photoUrl = photoUrl ?: existingUser.photoUrl,
                    email = email ?: existingUser.email,
                    updatedAt = currentTime
                )
            } else {
                User(
                    id = userId,
                    displayName = name,
                    photoUrl = photoUrl,
                    email = email,
                    isAnonymous = authRepository.isAnonymous(),
                    createdAt = currentTime,
                    updatedAt = currentTime
                )
            }
            
            userRepository.createOrUpdateUser(user)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка обновления имени")
        }
    }
}
