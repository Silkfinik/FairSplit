package com.silkfinik.fairsplit.core.domain.usecase.group

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.map
import com.silkfinik.fairsplit.core.domain.model.AppError
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.model.Currency
import javax.inject.Inject

class CreateGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(name: String, currency: Currency, imageUri: String?): Result<Unit> {
        val userId = authRepository.getUserId()
            ?: return Result.Error(AppError.Auth.NotAuthorized)

        return groupRepository.createGroup(name, currency, userId).map { groupId ->
            if (imageUri != null) {
                groupRepository.uploadGroupAvatar(groupId, imageUri)
            }
            Unit
        }
    }
}