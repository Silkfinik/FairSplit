package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.silkfinik.fairsplit.core.ui.theme.FairSplitShapes

@Composable
fun FairSplitSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.inverseSurface,
    contentColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
    actionColor: Color = MaterialTheme.colorScheme.inversePrimary,
    shape: Shape = FairSplitShapes.medium
) {
    Snackbar(
        modifier = modifier.padding(12.dp),
        action = {
            snackbarData.visuals.actionLabel?.let { actionLabel ->
                TextButton(
                    onClick = { snackbarData.performAction() },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = actionColor
                    )
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        containerColor = containerColor,
        contentColor = contentColor,
        shape = shape
    ) {
        Text(
            text = snackbarData.visuals.message,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}