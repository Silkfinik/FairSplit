package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class FairSplitButtonStyle {
    Primary,
    Secondary,
    Text
}

@Composable
fun FairSplitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: FairSplitButtonStyle = FairSplitButtonStyle.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val shape = RoundedCornerShape(12.dp)
    val heightModifier = if (style != FairSplitButtonStyle.Text) Modifier.height(50.dp) else Modifier

    val content: @Composable (Color) -> Unit = { contentColor ->
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                Text(text)
            }
        }
    }

    when (style) {
        FairSplitButtonStyle.Primary -> {
            Button(
                onClick = onClick,
                modifier = modifier.then(heightModifier),
                enabled = enabled && !isLoading,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                content(MaterialTheme.colorScheme.onPrimary)
            }
        }
        FairSplitButtonStyle.Secondary -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.then(heightModifier),
                enabled = enabled && !isLoading,
                shape = shape
            ) {
                content(MaterialTheme.colorScheme.primary)
            }
        }
        FairSplitButtonStyle.Text -> {
            TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled && !isLoading,
                shape = shape
            ) {
                content(MaterialTheme.colorScheme.primary)
            }
        }
    }
}