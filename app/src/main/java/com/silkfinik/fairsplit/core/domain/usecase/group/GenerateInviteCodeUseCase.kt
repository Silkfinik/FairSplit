package com.silkfinik.fairsplit.core.domain.usecase.group

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import javax.inject.Inject

class GenerateInviteCodeUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(groupId: String): Result<String> {
        return groupRepository.generateInviteCode(groupId)
    }
}