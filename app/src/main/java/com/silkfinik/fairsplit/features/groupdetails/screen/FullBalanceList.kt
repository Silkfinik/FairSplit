package com.silkfinik.fairsplit.features.groupdetails.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silkfinik.fairsplit.core.common.util.CurrencyFormatter
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.ui.component.UserAvatar
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
    val suggestedReceiverId = activeBalances.entries.filter { it.value > 0 }.maxByOrNull { it.value }?.key

    Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
        Text(
            text = "Детали баланса",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (activeBalances.isEmpty()) {
             Text("Все долги погашены.", style = MaterialTheme.typography.bodyLarge)
        } else {
            activeBalances.forEach { (memberId, balance) ->
                val member = members.find { it.id == memberId }
                val memberName = member?.name ?: "Неизвестный"
                val isCreditor = balance > 0
                val amountText = CurrencyFormatter.format(abs(balance), group.currency)
                val isMe = memberId == currentUserId

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            photoUrl = member?.photoUrl,
                            name = memberName,
                            size = 40.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = if (isMe) "$memberName (Вы)" else memberName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = if (isCreditor) "+$amountText" else "-$amountText",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCreditor) Color(0xFF006400) else Color(0xFFB00020)
                            )
                        }
                    }

                    if (isMe && !isCreditor) {
                        TextButton(
                            onClick = { onSettleUp(suggestedReceiverId, abs(balance).toString()) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Вернуть")
                        }
                    }
                }
            }
        }
    }
}