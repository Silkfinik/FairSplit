package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.silkfinik.fairsplit.core.common.util.CurrencyFormatter
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.model.enums.SplitType

@Composable
fun FairSplitMemberRow(
    member: Member,
    isSelected: Boolean,
    splitType: SplitType,
    amount: Double,
    inputValue: String,
    currency: Currency,
    onToggle: () -> Unit,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isCurrentUser: Boolean = false
) {
    val displayName = if (isCurrentUser) "${member.name} (Вы)" else member.name

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && splitType == SplitType.EQUAL) { onToggle() }
            .padding(vertical = 8.dp)
    ) {
        if (splitType == SplitType.EQUAL) {
            FairSplitCheckbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                enabled = enabled
            )
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }

        UserAvatar(
            photoUrl = member.photoUrl,
            name = member.name,
            size = 32.dp
        )
        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = displayName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )

        if (splitType == SplitType.EQUAL) {
            if (isSelected && amount > 0) {
                Text(
                    text = CurrencyFormatter.format(amount, currency),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (amount > 0 && splitType != SplitType.EXACT) {
                    Text(
                        text = CurrencyFormatter.format(amount, currency),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                FairSplitTextField(
                    value = inputValue,
                    onValueChange = onValueChange,
                    label = "",
                    modifier = Modifier.width(80.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = enabled,
                    placeholder = when(splitType) {
                        SplitType.PERCENT -> "%"
                        SplitType.SHARES -> "1"
                        else -> currency.symbol
                    }
                )
            }
        }
    }
}