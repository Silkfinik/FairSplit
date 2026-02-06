package com.silkfinik.fairsplit.core.domain.service

import com.silkfinik.fairsplit.core.model.Member
import javax.inject.Inject

class MemberResolutionService @Inject constructor() {

    inner class Resolution(
        private val redirectMap: Map<String, String>,
        private val ghostNamesMap: Map<String, List<String>>
    ) {
        fun resolveId(id: String): String = redirectMap[id] ?: id

        fun getMergedGhostNames(targetId: String): List<String> = ghostNamesMap[targetId] ?: emptyList()
    }

    fun resolve(members: List<Member>): Resolution {
        val redirectMap = members
            .filter { it.mergedWithUid != null }
            .associate { it.id to it.mergedWithUid!! }

        val ghostNamesMap = members
            .filter { it.mergedWithUid != null }
            .groupBy { it.mergedWithUid!! }
            .mapValues { entry -> entry.value.map { it.name } }

        return Resolution(redirectMap, ghostNamesMap)
    }

    fun getDisplayMembers(members: List<Member>): List<Member> {
        val resolution = resolve(members)

        return members
            .filter { it.mergedWithUid == null }
            .map { member ->
                val ghostNames = resolution.getMergedGhostNames(member.id)
                if (ghostNames.isNotEmpty()) {
                    member.copy(name = "${member.name} (${ghostNames.joinToString(", ")})")
                } else {
                    member
                }
            }
    }
}