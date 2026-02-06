package com.silkfinik.fairsplit.core.domain.usecase.member

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.TimeProvider
import com.silkfinik.fairsplit.core.domain.model.AppError
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.domain.repository.MemberRepository
import com.silkfinik.fairsplit.core.model.Member
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class AddGhostMemberUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(groupId: String, name: String): Result<Unit> {
        return try {
            val group = groupRepository.getGroup(groupId).first()
                ?: return Result.Error(AppError.Group.NotFound)

            val now = timeProvider.now()

            val newMember = Member(
                id = UUID.randomUUID().toString(),
                groupId = groupId,
                name = name,
                isGhost = true,
                createdAt = now,
                updatedAt = now
            )

            val addResult = memberRepository.addMember(newMember)
            if (addResult is Result.Error) return addResult

            groupRepository.updateGroup(group)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.Common.Unknown(e))
        }
    }
}