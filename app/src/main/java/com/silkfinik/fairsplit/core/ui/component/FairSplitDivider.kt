package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FairSplitDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 0.dp,
    endIndent: Dp = 0.dp,
    verticalSpacing: Dp = 0.dp,
    thickness: Dp = 1.dp,
    color: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
) {
    HorizontalDivider(
        modifier = modifier
            .padding(vertical = verticalSpacing)
            .padding(start = startIndent, end = endIndent),
        thickness = thickness,
        color = color
    )
}