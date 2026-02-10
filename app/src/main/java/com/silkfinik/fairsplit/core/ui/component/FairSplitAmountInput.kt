package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silkfinik.fairsplit.core.model.Currency

@Composable
fun FairSplitAmountInput(
    amount: String,
    onAmountChange: (String) -> Unit,
    currency: Currency,
    readOnly: Boolean,
    modifier: Modifier = Modifier
) {
    val targetFontSize = when (amount.length) {
        in 0..4 -> 57f
        in 5..7 -> 45f
        else -> 36f
    }

    val animatedFontSize by animateFloatAsState(
        targetValue = targetFontSize,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "FontSizeAnimation"
    )

    val baseStyle = MaterialTheme.typography.displayLarge.copy(
        fontSize = animatedFontSize.sp,
        lineHeight = animatedFontSize.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Start,
        textMotion = TextMotion.Animated
    )

    val textColor = MaterialTheme.colorScheme.onSurface
    val placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val cursorColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currency.symbol,
                style = baseStyle.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.alignByBaseline()
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(modifier = Modifier.alignByBaseline()) {
                if (amount.isEmpty()) {
                    Text(
                        text = "0",
                        style = baseStyle.copy(color = placeholderColor)
                    )
                }

                BasicTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        if (newValue.length <= 12 && newValue.all { it.isDigit() || it == '.' || it == ',' }) {
                            onAmountChange(newValue)
                        }
                    },
                    textStyle = baseStyle.copy(color = textColor),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    readOnly = readOnly,
                    cursorBrush = SolidColor(cursorColor),
                    interactionSource = remember { MutableInteractionSource() },
                    modifier = Modifier.width(IntrinsicSize.Min)
                )
            }
        }
    }
}