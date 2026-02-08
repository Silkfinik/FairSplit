package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FairSplitLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    LoadingIndicator(
        modifier = modifier,
        color = color,
        polygons = listOf(
            MaterialShapes.Cookie4Sided,
            MaterialShapes.Sunny,
            MaterialShapes.Cookie9Sided,
            MaterialShapes.VerySunny,
            MaterialShapes.Oval
        )
    )
}


@Preview
@Composable
fun PreviewLoader() {
    FairSplitLoader(modifier = Modifier.size(48.dp))
}