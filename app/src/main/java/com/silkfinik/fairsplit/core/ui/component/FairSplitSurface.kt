package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

enum class SurfaceStyle {
    Flat,
    Elevated,
    Bordered
}

@Composable
fun FairSplitSurface(
    modifier: Modifier = Modifier,
    style: SurfaceStyle = SurfaceStyle.Flat,
    shape: Shape = RectangleShape,
    color: Color? = null,
    content: @Composable () -> Unit
) {
    val backgroundColor = color ?: when (style) {
        SurfaceStyle.Flat -> MaterialTheme.colorScheme.surfaceContainer
        SurfaceStyle.Elevated -> MaterialTheme.colorScheme.surfaceContainerLow
        SurfaceStyle.Bordered -> Color.Transparent
    }

    val border = if (style == SurfaceStyle.Bordered) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    } else null

    val elevation = if (style == SurfaceStyle.Elevated) 2.dp else 0.dp

    Surface(
        modifier = modifier,
        shape = shape,
        color = backgroundColor,
        contentColor = contentColorFor(backgroundColor),
        tonalElevation = 0.dp,
        shadowElevation = elevation,
        border = border,
        content = content
    )
}