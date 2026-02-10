package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.silkfinik.fairsplit.core.ui.theme.FairSplitShapes

@Composable
fun FairSplitCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "CheckboxScale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (checked)
            MaterialTheme.colorScheme.primary
        else
            Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "CheckboxBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (checked)
            Color.Transparent
        else
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        animationSpec = tween(durationMillis = 200),
        label = "CheckboxBorder"
    )

    val checkboxSize = 24.dp
    val shape: Shape = FairSplitShapes.small

    Box(
        modifier = modifier
            .requiredSize(48.dp)
            .clip(MaterialTheme.shapes.small)
            .toggleable(
                value = checked,
                onValueChange = { newValue ->
                    if (onCheckedChange != null) {
                        onCheckedChange(newValue)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                enabled = enabled && onCheckedChange != null,
                role = Role.Checkbox,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 24.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .size(checkboxSize)
                .clip(shape)
                .background(backgroundColor)
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = checked,
                enter = scaleIn(initialScale = 0.5f) + fadeIn(),
                exit = scaleOut(targetScale = 0.5f) + fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
