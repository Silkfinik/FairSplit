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
        val userId = authRepository.getUserId() ?: return Result.Error("Не авторизован")

        return when (val result = groupRepository.createGroup(name, currency, userId)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(result.message, result.exception)
            is Result.Loading -> Result.Loading
        }
    }
}