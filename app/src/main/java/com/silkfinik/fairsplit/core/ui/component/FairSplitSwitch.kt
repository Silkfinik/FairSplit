package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun FairSplitSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showIcons: Boolean = true
) {
    val trackWidth = 52.dp
    val trackHeight = 32.dp
    val thumbSize = 24.dp
    val thumbPadding = 4.dp

    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    val trackColor by animateColorAsState(
        targetValue = if (checked)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "TrackColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (checked)
            Color.Transparent
        else
            MaterialTheme.colorScheme.outline,
        label = "BorderColor"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.outline,
        label = "ThumbColor"
    )

    val alignmentBias by animateFloatAsState(
        targetValue = if (checked) 1f else -1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "ThumbBias"
    )

    val trackShape: Shape = CircleShape

    Box(
        modifier = modifier
            .requiredSize(width = trackWidth, height = trackHeight)
            .clip(trackShape)
            .background(trackColor)
            .then(
                if (!checked) Modifier.border(2.dp, borderColor, trackShape) else Modifier
            )
            .toggleable(
                value = checked,
                onValueChange = {
                    onCheckedChange(it)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null
            ),
        contentAlignment = BiasAlignment(horizontalBias = alignmentBias, verticalBias = 0f)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = thumbPadding)
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor),
            contentAlignment = Alignment.Center
        ) {
            if (showIcons) {
                AnimatedContent(
                    targetState = checked,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.5f)) togetherWith
                                fadeOut(animationSpec = tween(200))
                    },
                    label = "SwitchIcon"
                ) { isChecked ->
                    if (isChecked) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }
                }
            }
        }
    }
}