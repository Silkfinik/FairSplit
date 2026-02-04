package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.silkfinik.fairsplit.core.common.util.CurrencyFormatter
import com.silkfinik.fairsplit.core.model.Currency

@Composable
fun FairSplitMoneyText(
    amount: Double,
    currency: Currency,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    fontWeight: FontWeight = FontWeight.Bold,
    useColor: Boolean = true
) {
    val formattedAmount = CurrencyFormatter.format(amount, currency)

    val color = if (useColor) {
        when {
            amount > 0.009 -> Color(0xFF4CAF50)
            amount < -0.009 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
        }
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Text(
        text = formattedAmount,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight
    )
}