package com.silkfinik.fairsplit.core.domain.usecase.group

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.model.Currency
import javax.inject.Inject

class CreateGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(name: String, currency: Currency): Result<Unit> {
        return try {
            val userId = authRepository.getUserId() ?: return Result.Error("Не авторизован")
            groupRepository.createGroup(name, currency, userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка при создании группы", e)
        }
    }
}
