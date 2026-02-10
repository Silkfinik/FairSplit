package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.silkfinik.fairsplit.core.ui.theme.FairSplitShapes

@Composable
fun FairSplitCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    onClick: (() -> Unit)? = null,
    shape: Shape = FairSplitShapes.large,
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "CardScale"
    )

    val cardColors = CardDefaults.cardColors(
        containerColor = backgroundColor,
        contentColor = contentColorFor(backgroundColor)
    )

    val scaleModifier = Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .then(scaleModifier),
            shape = shape,
            colors = cardColors,
            elevation = elevation,
            interactionSource = interactionSource,
            content = content
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = cardColors,
            elevation = elevation,
            content = content
        )
    }
}
