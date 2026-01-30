package com.silkfinik.fairsplit.features.expenses.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.core.common.util.CurrencyFormatter
import com.silkfinik.fairsplit.core.common.util.asSafeMap
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.HistoryItem
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.model.enums.ExpenseCategory
import com.silkfinik.fairsplit.core.ui.component.FairSplitCard
import com.silkfinik.fairsplit.core.ui.component.FairSplitTopAppBar
import com.silkfinik.fairsplit.features.expenses.viewmodel.ExpenseHistoryUiState
import com.silkfinik.fairsplit.features.expenses.viewmodel.ExpenseHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseHistoryScreen(
    viewModel: ExpenseHistoryViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            FairSplitTopAppBar(title = "История изменений", onBackClick = onBack)
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                ExpenseHistoryUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ExpenseHistoryUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is ExpenseHistoryUiState.Success -> {
                    HistoryList(
                        history = state.history,
                        members = state.members,
                        currency = state.currency
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryList(
    history: List<HistoryItem>,
    members: Map<String, Member>,
    currency: Currency
) {
    if (history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("История пуста", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        val groupedHistory = remember(history) {
            history.groupBy { formatDateHeader(it.timestamp) }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            groupedHistory.forEach { (dateHeader, items) ->
                stickyHeader {
                    DateHeader(dateHeader)
                }
                
                items(
                    items = items,
                    key = { it.id }
                ) { item ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                    ) {
                        HistoryItemCard(item = item, members = members, currency = currency)
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeader(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
fun HistoryItemCard(
    item: HistoryItem,
    members: Map<String, Member>,
    currency: Currency
) {
    val isCreate = item.action == "CREATE" || item.changes.containsKey("_event")
    
    FairSplitCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HistoryIcon(isCreate = isCreate)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCreate) "Расход создан" else "Расход изменен",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatTime(item.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            if (isCreate) {
                CreateContent(item.changes, members, currency)
            } else {
                UpdateContent(item.changes, members, currency)
            }

            if (!item.isMathValid) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small)
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Ошибка",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Обнаружена ошибка в расчетах",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryIcon(isCreate: Boolean) {
    val backgroundColor = if (isCreate) 
        MaterialTheme.colorScheme.primaryContainer 
    else 
        MaterialTheme.colorScheme.secondaryContainer
        
    val iconColor = if (isCreate)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSecondaryContainer
        
    val icon = if (isCreate) Icons.Default.AddCircle else Icons.Default.Edit

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun CreateContent(
    changes: Map<String, Any>, 
    members: Map<String, Member>,
    currency: Currency
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        changes["description"]?.let {
            Text(text = "Описание: $it", style = MaterialTheme.typography.bodyMedium)
        }
        changes["amount"]?.let {
            val amount = (it as? Number)?.toDouble() ?: 0.0
            Text(
                text = "Сумма: ${CurrencyFormatter.format(amount, currency)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        val payers = changes["payers"].asSafeMap()
        if (payers.isNotEmpty()) {
             Spacer(modifier = Modifier.height(4.dp))
             Text("Плательщики:", style = MaterialTheme.typography.labelMedium)
             payers.forEach { (id, amount) ->
                 val memberName = members[id]?.name ?: "Unknown"
                 val amountVal = (amount as? Number)?.toDouble() ?: 0.0
                 Text(
                     text = "• $memberName: ${CurrencyFormatter.format(amountVal, currency)}",
                     style = MaterialTheme.typography.bodySmall
                 )
             }
        }

        val splits = changes["splits"].asSafeMap()
        if (splits.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Разделение:", style = MaterialTheme.typography.labelMedium)
            splits.forEach { (id, amount) ->
                val memberName = members[id]?.name ?: "Unknown"
                val amountVal = (amount as? Number)?.toDouble() ?: 0.0
                Text(
                    text = "• $memberName: ${CurrencyFormatter.format(amountVal, currency)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun UpdateContent(
    changes: Map<String, Any>, 
    members: Map<String, Member>,
    currency: Currency
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        changes.forEach { (field, changeValue) ->
            if (field == "_event" || field == "is_math_valid" || field == "server_validated_at") return@forEach
            
            val changeMap = changeValue.asSafeMap()
            val from = changeMap["from"]
            val to = changeMap["to"]
            
            when (field) {
                "amount" -> {
                    val fromVal = (from as? Number)?.toDouble()
                    val toVal = (to as? Number)?.toDouble()
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Сумма: ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(100.dp)
                        )
                        Column {
                            if (fromVal != null) {
                                Text(
                                    text = CurrencyFormatter.format(fromVal, currency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                )
                            }
                            if (toVal != null) {
                                Text(
                                    text = CurrencyFormatter.format(toVal, currency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                "description" -> {
                    ChangeRow("Описание", from.toString(), to.toString())
                }
                "category" -> {
                     val fromCategory = ExpenseCategory.fromId(from as? String)
                     val toCategory = ExpenseCategory.fromId(to as? String)
                     
                     Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Категория: ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(100.dp)
                        )
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = fromCategory.icon, 
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = fromCategory.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                )
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = toCategory.icon, 
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = toCategory.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                "payers" -> {
                    val fromMap = from.asSafeMap()
                    val toMap = to.asSafeMap()
                    MapChangeSection("Плательщики", fromMap, toMap, members, currency)
                }
                "splits" -> {
                    val fromMap = from.asSafeMap()
                    val toMap = to.asSafeMap()
                    MapChangeSection("Разделение", fromMap, toMap, members, currency)
                }
                else -> {

                }
            }
        }
    }
}

@Composable
private fun ChangeRow(label: String, from: String?, to: String?) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp)
        )
        Column {
            if (from != null && from != "null") {
                Text(
                    text = from,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                )
            }
            if (to != null && to != "null") {
                Text(
                    text = to,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MapChangeSection(
    title: String,
    from: Map<String, Any>,
    to: Map<String, Any>,
    members: Map<String, Member>,
    currency: Currency
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Column {
        Text(
            text = "$title:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        val allKeys = from.keys + to.keys
        val significantChanges = allKeys.distinct().filter { key ->
            val fromVal = (from[key] as? Number)?.toDouble()
            val toVal = (to[key] as? Number)?.toDouble()
            fromVal != toVal
        }
        
        val itemsToShow = if (isExpanded) significantChanges else significantChanges.take(3)

        itemsToShow.forEach { key ->
            val fromVal = (from[key] as? Number)?.toDouble()
            val toVal = (to[key] as? Number)?.toDouble()
            val memberName = members[key]?.name ?: "Unknown"

            Row(
                modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "• $memberName: ",
                    style = MaterialTheme.typography.bodySmall
                )
                if (fromVal != null) {
                    Text(
                        text = CurrencyFormatter.format(fromVal, currency),
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(horizontal = 4.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (toVal != null) {
                    Text(
                        text = CurrencyFormatter.format(toVal, currency),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "(удален)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (significantChanges.size > 3) {
            TextButton(
                onClick = { isExpanded = !isExpanded },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = if (isExpanded) "Свернуть" else "Показать еще (${significantChanges.size - 3})",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

private fun formatDateHeader(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Calendar.getInstance()
    val itemDate = Calendar.getInstance().apply { time = date }
    
    return when {
        isSameDay(now, itemDate) -> "Сегодня"
        isYesterday(now, itemDate) -> "Вчера"
        else -> SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(date)
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(now: Calendar, itemDate: Calendar): Boolean {
    val yesterday = now.clone() as Calendar
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    return isSameDay(yesterday, itemDate)
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}


