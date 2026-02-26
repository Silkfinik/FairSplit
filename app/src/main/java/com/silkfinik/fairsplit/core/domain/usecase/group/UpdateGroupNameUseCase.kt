package com.silkfinik.fairsplit.core.domain.usecase.group

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.model.AppError
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class UpdateGroupNameUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(groupId: String, newName: String): Result<Unit> {
        val group = groupRepository.getGroup(groupId).firstOrNull()
            ?: return Result.Error(AppError.Group.NotFound)

        val updatedGroup = group.copy(name = newName)
        return groupRepository.updateGroup(updatedGroup)
    }
}
