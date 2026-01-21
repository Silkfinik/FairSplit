package com.silkfinik.fairsplit.core.domain.usecase.group

import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.model.Group
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    operator fun invoke(groupId: String): Flow<Group?> {
        return groupRepository.getGroup(groupId)
    }
}
