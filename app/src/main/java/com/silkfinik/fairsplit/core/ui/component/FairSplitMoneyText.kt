package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.silkfinik.fairsplit.core.common.util.CurrencyFormatter
import com.silkfinik.fairsplit.core.model.Currency
import kotlin.math.absoluteValue

@Composable
fun FairSplitMoneyText(
    amount: Double,
    currency: Currency,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight = FontWeight.Bold,
    useColor: Boolean = true,
    showSign: Boolean = false
) {
    val successColor = if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF1B5E20)

    val color = if (useColor) {
        when {
            amount > 0.009 -> successColor
            amount < -0.009 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    } else {
        Color.Unspecified
    }

    val formattedAmount = CurrencyFormatter.format(amount.absoluteValue, currency)
    val prefix = if (showSign && amount > 0.009) "+" else if (amount < -0.009) "-" else ""
    val finalText = "$prefix$formattedAmount"

    Text(
        text = finalText,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.End
    )
}