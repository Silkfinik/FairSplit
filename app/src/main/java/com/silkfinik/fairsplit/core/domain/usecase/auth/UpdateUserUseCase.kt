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
    suspend operator fun invoke(name: String, photoUrl: String? = null): Result<Unit> {
        return try {
            val authResult = authRepository.updateProfile(name, photoUrl)
            if (authResult is Result.Error) {
                return authResult
            }

            val userId = authRepository.getUserId() ?: return Result.Error("Пользователь не найден")
            
            val user = User(
                id = userId,
                displayName = name,
                photoUrl = photoUrl,
                updatedAt = System.currentTimeMillis()
            )
            userRepository.createOrUpdateUser(user)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка обновления имени")
        }
    }
}
