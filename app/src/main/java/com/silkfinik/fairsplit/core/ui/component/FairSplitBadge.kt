package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class BadgeType {
    Primary,
    Secondary,
    Info,
    Neutral,
    Success,
    Error,
    Warning
}

@Immutable
data class FairSplitBadgeColors(
    val containerColor: Color,
    val contentColor: Color
)

object FairSplitBadgeDefaults {
    private val WarningContainerLight = Color(0xFFFFCC80)
    private val OnWarningContainerLight = Color(0xFFE65100)

    private val WarningContainerDark = Color(0xFF533004)
    private val OnWarningContainerDark = Color(0xFFFFCC80)

    @Composable
    @ReadOnlyComposable
    fun colors(type: BadgeType): FairSplitBadgeColors {
        val colorScheme = MaterialTheme.colorScheme
        val isDark = isSystemInDarkTheme()

        val (container, content) = when (type) {
            BadgeType.Primary -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer

            BadgeType.Secondary -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer

            BadgeType.Info -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer

            BadgeType.Neutral -> colorScheme.surfaceContainerHigh to colorScheme.onSurface

            BadgeType.Success -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer

            BadgeType.Error -> colorScheme.errorContainer to colorScheme.onErrorContainer

            BadgeType.Warning -> if (isDark) {
                WarningContainerDark to OnWarningContainerDark
            } else {
                WarningContainerLight to OnWarningContainerLight
            }
        }

        return FairSplitBadgeColors(container, content)
    }
}

@Composable
fun FairSplitBadge(
    text: String,
    modifier: Modifier = Modifier,
    type: BadgeType = BadgeType.Neutral,
    shape: Shape = MaterialTheme.shapes.extraSmall,
    colors: FairSplitBadgeColors = FairSplitBadgeDefaults.colors(type)
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = colors.containerColor,
        contentColor = colors.contentColor,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}