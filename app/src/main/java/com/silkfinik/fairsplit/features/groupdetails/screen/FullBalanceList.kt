package com.silkfinik.fairsplit.features.groupdetails.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.ui.component.BadgeType
import com.silkfinik.fairsplit.core.ui.component.FairSplitBadge
import com.silkfinik.fairsplit.core.ui.component.FairSplitDivider
import com.silkfinik.fairsplit.core.ui.component.FairSplitListItem
import com.silkfinik.fairsplit.core.ui.component.FairSplitMoneyText
import com.silkfinik.fairsplit.core.ui.component.FairSplitUserAvatar
import kotlin.math.abs

@Composable
fun FullBalanceList(
    balances: Map<String, Double>,
    members: List<Member>,
    group: Group,
    currentUserId: String?,
    onSettleUp: (String?, String?) -> Unit
) {
    val activeBalances = balances.filter { abs(it.value) > 0.01 }
        .toList()
        .sortedByDescending { it.second }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Детали баланса",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        FairSplitDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            startIndent = 24.dp,
            endIndent = 24.dp
        )

        if (activeBalances.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF1B5E20),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Все долги погашены",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "В этой группе никто никому не должен.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                activeBalances.forEach { (memberId, balance) ->
                    val member = members.find { it.id == memberId }
                    val memberName = member?.name ?: "Неизвестный"
                    val isMe = memberId == currentUserId

                    FairSplitListItem(
                        modifier = Modifier.fillMaxWidth(),
                        leadingContent = {
                            FairSplitUserAvatar(
                                photoUrl = member?.photoUrl,
                                name = memberName,
                                size = 48.dp
                            )
                        },
                        headlineContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = memberName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (isMe) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FairSplitBadge(text = "Вы", type = BadgeType.Secondary)
                                }
                            }
                        },
                        // supportingContent убран полностью
                        trailingContent = {
                            // Всегда показываем только сумму, без кнопок
                            FairSplitMoneyText(
                                amount = balance,
                                currency = group.currency,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                showSign = true,
                                useColor = true
                            )
                        }
                    )
                }
            }
        }
    }
}