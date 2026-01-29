package com.silkfinik.fairsplit.core.data.mapper

import com.silkfinik.fairsplit.core.database.entity.MemberEntity
import com.silkfinik.fairsplit.core.network.model.GhostDto

fun MemberEntity.asGhostDto(): GhostDto {
    return GhostDto(
        name = this.name,
        isMerged = false,
        mergedWithUid = null
    )
}

fun GhostDto.asMemberEntity(id: String, groupId: String): MemberEntity {
    return MemberEntity(
        id = id,
        groupId = groupId,
        name = this.name,
        isGhost = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        isDirty = false
    )
}
