package com.silkfinik.fairsplit.core.data.mapper

import com.silkfinik.fairsplit.core.database.entity.MemberEntity
import com.silkfinik.fairsplit.core.network.model.GhostDto

fun MemberEntity.asGhostDto(): GhostDto {
    return GhostDto(
        name = this.name,
        isMerged = false, // Default for now, logic will be added with Smart Linking
        mergedWithUid = null
    )
}

fun GhostDto.asMemberEntity(id: String, groupId: String): MemberEntity {
    return MemberEntity(
        id = id,
        group_id = groupId,
        name = this.name,
        is_ghost = true,
        created_at = System.currentTimeMillis(), // Not present in GhostDto map
        updated_at = System.currentTimeMillis(),
        is_dirty = false // Coming from server
    )
}
