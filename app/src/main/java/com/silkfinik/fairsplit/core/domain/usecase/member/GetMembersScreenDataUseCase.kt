package com.silkfinik.fairsplit.core.domain.usecase.member

import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.domain.repository.MemberRepository
import com.silkfinik.fairsplit.core.domain.repository.UserRepository
import com.silkfinik.fairsplit.core.model.Member
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class GetMembersScreenDataUseCase @Inject constructor(
    private val memberRepository: MemberRepository,
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    data class ScreenData(
        val members: List<Member>,
        val currentUserId: String?,
        val linkedGhostIds: List<String>,
        val hasClaimedGhost: Boolean,
        val inviteCode: String?
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(groupId: String): Flow<ScreenData> {
        return combine(
            memberRepository.getMembers(groupId),
            groupRepository.getGroup(groupId),
            authRepository.currentUserId.flatMapLatest { uid ->
                if (uid != null) userRepository.getUser(uid) else flowOf(null)
            }
        ) { members, group, user ->
            val userId = user?.id
            val linkedIds = user?.linkedGhostIds ?: emptyList()

            val hasClaimed = members.any { member ->
                linkedIds.contains(member.id) || (userId != null && member.mergedWithUid == userId)
            }

            ScreenData(
                members = members,
                currentUserId = userId,
                linkedGhostIds = linkedIds,
                hasClaimedGhost = hasClaimed,
                inviteCode = group?.inviteCode
            )
        }
    }
}