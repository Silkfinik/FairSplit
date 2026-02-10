package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.silkfinik.fairsplit.core.model.Member

@Composable
fun FairSplitUserPill(
    member: Member,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showNameWhenSelected: Boolean = true
) {
    val expressiveSpatialSpec = spring<IntSize>(
        dampingRatio = 0.8f,
        stiffness = 380f
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = modifier.animateContentSize(animationSpec = expressiveSpatialSpec)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp)
        ) {
            FairSplitUserAvatar(
                photoUrl = member.photoUrl,
                name = member.name,
                size = 32.dp
            )
            if (isSelected && showNameWhenSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.labelLarge,
                    //fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}