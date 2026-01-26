package com.silkfinik.fairsplit.core.domain.usecase.auth

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.UserRepository
import com.silkfinik.fairsplit.core.model.User
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateUserNameUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(name: String): Result<Unit> {
        return try {
            // 1. Update Firebase Auth Profile
            val authResult = authRepository.updateDisplayName(name)
            if (!authResult.isSuccess) {
                return Result.Error(authResult.exceptionOrNull()?.message ?: "Ошибка обновления профиля")
            }

            // 2. Update Firestore User Document
            val userId = authRepository.getUserId() ?: return Result.Error("Пользователь не найден")
            
            // Fetch existing user to preserve fields
            val currentUser = userRepository.getUser(userId).first()
            
            val isAnon = authRepository.isAnonymous()
            
            val updatedUser = if (currentUser != null) {
                currentUser.copy(
                    displayName = name, 
                    isAnonymous = isAnon,
                    createdAt = if (currentUser.createdAt == 0L) System.currentTimeMillis() else currentUser.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                User(
                    id = userId,
                    displayName = name,
                    isAnonymous = isAnon,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }
            
            userRepository.createOrUpdateUser(updatedUser)
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка обновления имени")
        }
    }
}
