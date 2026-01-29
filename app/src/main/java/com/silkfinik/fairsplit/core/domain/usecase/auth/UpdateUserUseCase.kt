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
            if (!authResult.isSuccess) {
                return Result.Error(authResult.exceptionOrNull()?.message ?: "Ошибка обновления профиля")
            }

            val userId = authRepository.getUserId() ?: return Result.Error("Пользователь не найден")

            val currentUser = userRepository.getUser(userId).first()
            
            val isAnon = authRepository.isAnonymous()
            
            val updatedUser = currentUser?.copy(
                displayName = name,
                photoUrl = photoUrl ?: currentUser.photoUrl,
                isAnonymous = isAnon,
                createdAt = if (currentUser.createdAt == 0L) System.currentTimeMillis() else currentUser.createdAt,
                updatedAt = System.currentTimeMillis()
            )
                ?: User(
                    id = userId,
                    displayName = name,
                    photoUrl = photoUrl,
                    isAnonymous = isAnon,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            
            userRepository.createOrUpdateUser(updatedUser)
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка обновления имени")
        }
    }
}
