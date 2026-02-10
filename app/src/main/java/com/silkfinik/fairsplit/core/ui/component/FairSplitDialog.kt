package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun FairSplitDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    text: String? = null,
    content: @Composable (() -> Unit)? = null,
    icon: ImageVector? = null,
    confirmLabel: String = "OK",
    onConfirmAction: () -> Unit,
    dismissLabel: String? = "Отмена",
    onDismissAction: (() -> Unit)? = onDismissRequest,
    isLoading: Boolean = false,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = DialogProperties(
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = !isLoading
        ),
        icon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                )
            }
        } else null,

        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },

        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (text != null) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (content != null) {
                    if (text != null) Spacer(modifier = Modifier.height(16.dp))
                    content()
                }
            }
        },

        confirmButton = {
            if (isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    FairSplitLoader(modifier = Modifier.size(24.dp))
                }
            } else {
                val buttonColors = if (isDestructive) {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                }

                TextButton(
                    onClick = onConfirmAction,
                    colors = buttonColors
                ) {
                    Text(
                        text = confirmLabel,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = {
            if (!isLoading && dismissLabel != null && onDismissAction != null) {
                TextButton(
                    onClick = onDismissAction,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(text = dismissLabel)
                }
            }
        },

        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge
    )
}