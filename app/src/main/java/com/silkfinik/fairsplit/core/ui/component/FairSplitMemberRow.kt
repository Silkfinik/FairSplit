package com.silkfinik.fairsplit.core.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    isCurrentUser: Boolean = false,
    showDivider: Boolean = true
) {
    val isEqualSplit = splitType == SplitType.EQUAL

    Column(modifier = modifier) {
        FairSplitListItem(
            modifier = Modifier.fillMaxWidth(),
            onClick = if (enabled && isEqualSplit) onToggle else null,

            leadingContent = {
                FairSplitUserAvatar(
                    photoUrl = member.photoUrl,
                    name = member.name,
                    size = 40.dp
                )
            },

            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (isSelected || !isEqualSplit) FontWeight.Normal else FontWeight.Light,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        FairSplitBadge(
                            text = "ВЫ",
                            type = BadgeType.Secondary
                        )
                    }
                }
            },

            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.defaultMinSize(minHeight = 56.dp)
                ) {
                    if (isEqualSplit) {
                        if (isSelected && amount > 0) {
                            FairSplitMoneyText(
                                amount = amount,
                                currency = currency,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                        FairSplitCheckbox(
                            checked = isSelected,
                            onCheckedChange = { onToggle() },
                            enabled = enabled
                        )
                    } else {
                        val suffixText = when (splitType) {
                            SplitType.PERCENT -> "%"
                            SplitType.SHARES -> "#"
                            else -> currency.symbol
                        }

                        val targetFontSize = when {
                            inputValue.length > 9 -> 12f
                            inputValue.length > 6 -> 14f
                            else -> 16f
                        }

                        val animatedSize by animateFloatAsState(
                            targetValue = targetFontSize,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "RowFontSizeAnimation"
                        )

                        val flexibleTextStyle = MaterialTheme.typography.titleMedium.copy(
                            fontSize = animatedSize.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.End
                        )

                        FairSplitTextField(
                            value = inputValue,
                            onValueChange = { newValue ->
                                if (newValue.length <= 12) {
                                    onValueChange(newValue)
                                }
                            },
                            label = null,
                            modifier = Modifier.width(120.dp),
                            textStyle = flexibleTextStyle,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = enabled,
                            suffix = {
                                Text(
                                    text = suffixText,
                                    style = if (inputValue.length > 9) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "0",
                                    style = flexibleTextStyle,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        )
                    }
                }
            }
        )

        if (showDivider) {
            FairSplitDivider(
                startIndent = 72.dp,
                endIndent = 16.dp
            )
        }
    }
}