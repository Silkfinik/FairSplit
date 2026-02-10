package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FairSplitListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    colors: ListItemColors = ListItemDefaults.colors(),
    swipeEnabled: Boolean = false,
    startActionIcon: ImageVector? = null,
    startActionLabel: String? = null,
    endActionIcon: ImageVector? = null,
    endActionLabel: String? = null,
    onSwipeStart: (() -> Unit)? = null,
    onSwipeEnd: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "ListItemScale"
    )

    val contentModifier = Modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )

    val itemContent = @Composable {
        ListItem(
            headlineContent = headlineContent,
            modifier = contentModifier,
            supportingContent = supportingContent,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            overlineContent = overlineContent,
            colors = colors,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        )
    }

    if (swipeEnabled) {
        val currentOnSwipeStart by rememberUpdatedState(onSwipeStart)
        val currentOnSwipeEnd by rememberUpdatedState(onSwipeEnd)

        val dismissState = rememberSwipeToDismissBoxState(
            positionalThreshold = { totalDistance -> totalDistance * 0.4f }
        )

        LaunchedEffect(dismissState.currentValue) {
            when (dismissState.currentValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    currentOnSwipeStart?.invoke()
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    currentOnSwipeEnd?.invoke()
                }
                SwipeToDismissBoxValue.Settled -> {}
            }
        }

        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val color = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                }

                // Контент фона (Иконка + Текст)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color)
                        .padding(horizontal = 24.dp),
                    contentAlignment = when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                        else -> Alignment.Center
                    }
                ) {
                    if (direction == SwipeToDismissBoxValue.StartToEnd && (startActionIcon != null || startActionLabel != null)) {
                        SwipeActionContent(
                            icon = startActionIcon,
                            label = startActionLabel,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else if (direction == SwipeToDismissBoxValue.EndToStart && (endActionIcon != null || endActionLabel != null)) {
                        SwipeActionContent(
                            icon = endActionIcon,
                            label = endActionLabel,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            },
            modifier = modifier,
            content = { itemContent() }
        )
    } else {
        Box(modifier = modifier) {
            itemContent()
        }
    }
}

@Composable
private fun SwipeActionContent(
    icon: ImageVector?,
    label: String?,
    contentColor: Color
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null, // Label используется для описания
                tint = contentColor,
                modifier = Modifier.scale(1.1f)
            )
        }
        if (label != null) {
            if (icon != null) Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}