package com.silkfinik.fairsplit.core.domain.usecase.member

import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.domain.repository.MemberRepository
import com.silkfinik.fairsplit.core.model.Member
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class AddGhostMemberUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository
) {
    suspend operator fun invoke(groupId: String, name: String): Result<Unit> {
        return try {
            val group = groupRepository.getGroup(groupId).first() 
                ?: return Result.Error("Группа не найдена")
            
            val newMember = Member(
                id = UUID.randomUUID().toString(),
                groupId = groupId,
                name = name,
                isGhost = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            memberRepository.addMember(newMember)
            
            // Mark group as dirty to trigger sync
            groupRepository.updateGroup(group)
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка при добавлении участника", e)
        }
    }
}
