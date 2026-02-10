package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FairSplitIconButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors()
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "IconButtonScale"
    )

    IconButton(
        onClick = {
            if (!loading) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        },
        modifier = modifier.scale(scale),
        enabled = enabled && !loading,
        colors = colors,
        interactionSource = interactionSource
    ) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = {
                scaleIn(spring(stiffness = 300f)) togetherWith scaleOut(spring(stiffness = 300f))
            },
            label = "IconLoadAnimation"
        ) { isLoading ->
            if (isLoading) {
                Box(contentAlignment = Alignment.Center) {
                    FairSplitLoader(
                        modifier = Modifier.size(24.dp),
                        color = LocalContentColor.current
                    )
                }
            } else {
                icon()
            }
        }
    }
}

@Composable
fun FairSplitIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    tint: Color = LocalContentColor.current,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors()
) {
    FairSplitIconButton(
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint
            )
        },
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        colors = colors
    )
}