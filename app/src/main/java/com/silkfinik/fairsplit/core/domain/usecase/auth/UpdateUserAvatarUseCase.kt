package com.silkfinik.fairsplit.core.domain.usecase.auth

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateUserAvatarUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val updateUserUseCase: UpdateUserUseCase
) {
    suspend operator fun invoke(imageUri: String): Result<Unit> {
        val uid = authRepository.getUserId() ?: return Result.Error("Пользователь не найден")

        val uploadResult = userRepository.uploadUserAvatar(uid, imageUri)

        if (uploadResult is Result.Error) {
            return Result.Error(uploadResult.message, uploadResult.exception)
        }

        val downloadUrl = (uploadResult as Result.Success).data

        val currentUser = userRepository.getUser(uid).first()

        val currentName = currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: authRepository.getUserName()
            ?: "User"

        return updateUserUseCase(name = currentName, photoUrl = downloadUrl)
    }
}