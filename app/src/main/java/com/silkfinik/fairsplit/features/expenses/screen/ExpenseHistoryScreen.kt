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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.R
import com.silkfinik.fairsplit.core.common.util.CurrencyFormatter
import com.silkfinik.fairsplit.core.common.util.asSafeMap
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.HistoryItem
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.model.enums.ExpenseCategory
import com.silkfinik.fairsplit.core.ui.common.ObserveAsEvents
import com.silkfinik.fairsplit.core.ui.component.FairSplitCard
import com.silkfinik.fairsplit.core.ui.component.FairSplitEmptyState
import com.silkfinik.fairsplit.core.ui.component.FairSplitScaffold
import com.silkfinik.fairsplit.core.ui.component.FairSplitTopAppBar
import com.silkfinik.fairsplit.features.expenses.ui.ExpenseHistoryUiState
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
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveAsEvents(
        flow = viewModel.uiEvent,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onBack
    )

    FairSplitScaffold(
        snackbarHostState = snackbarHostState,
        topBar = {
            FairSplitTopAppBar(title = stringResource(R.string.history_title), onBackClick = onBack)
        },
        isLoading = uiState is ExpenseHistoryUiState.Loading
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                ExpenseHistoryUiState.Loading -> { }
                is ExpenseHistoryUiState.Error -> {
                    FairSplitEmptyState(
                        modifier = Modifier.align(Alignment.Center),
                        icon = Icons.Default.Warning,
                        title = stringResource(R.string.group_details_error_title),
                        description = state.message.asString(context),
                        actionLabel = stringResource(R.string.action_back),
                        onActionClick = onBack
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
        FairSplitEmptyState(
            icon = Icons.Default.History,
            title = stringResource(R.string.history_empty_title),
            description = stringResource(R.string.history_empty_desc)
        )
    } else {
        val todayStr = stringResource(R.string.history_date_today)
        val yesterdayStr = stringResource(R.string.history_date_yesterday)

        val groupedHistory = remember(history, todayStr, yesterdayStr) {
            history.groupBy { formatDateHeader(it.timestamp, todayStr, yesterdayStr) }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
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
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 12.dp)
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
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCreate) stringResource(R.string.history_action_created) else stringResource(R.string.history_action_updated),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatTime(item.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isCreate) {
                CreateContent(item.changes, members, currency)
            } else {
                UpdateContent(item.changes, members, currency)
            }

            if (!item.isMathValid) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium)
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = stringResource(R.string.transaction_expense_error),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.history_error_math),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
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
        MaterialTheme.colorScheme.tertiaryContainer

    val iconColor = if (isCreate)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onTertiaryContainer

    val icon = if (isCreate) Icons.Default.AddCircle else Icons.Default.Edit

    Box(
        modifier = Modifier
            .size(48.dp)
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        changes["description"]?.let {
            Text(text = "${stringResource(R.string.history_label_description)}: $it", style = MaterialTheme.typography.bodyMedium)
        }
        changes["amount"]?.let {
            val amount = (it as? Number)?.toDouble() ?: 0.0
            Text(
                text = "${stringResource(R.string.history_label_amount)}: ${CurrencyFormatter.format(amount, currency)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        val payers = changes["payers"].asSafeMap()
        if (payers.isNotEmpty()) {
            Text("${stringResource(R.string.history_label_payers)}:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            payers.forEach { (id, amount) ->
                val memberName = members[id]?.name ?: "Unknown"
                val amountVal = (amount as? Number)?.toDouble() ?: 0.0
                Text(
                    text = "• $memberName: ${CurrencyFormatter.format(amountVal, currency)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        val splits = changes["splits"].asSafeMap()
        if (splits.isNotEmpty()) {
            Text("${stringResource(R.string.history_label_splits)}:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            splits.forEach { (id, amount) ->
                val memberName = members[id]?.name ?: "Unknown"
                val amountVal = (amount as? Number)?.toDouble() ?: 0.0
                Text(
                    text = "• $memberName: ${CurrencyFormatter.format(amountVal, currency)}",
                    style = MaterialTheme.typography.bodyMedium
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            text = "${stringResource(R.string.history_label_amount)}: ",
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
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                "description" -> ChangeRow(stringResource(R.string.history_label_description), from.toString(), to.toString())
                "category" -> {
                    val fromCategory = ExpenseCategory.fromId(from as? String)
                    val toCategory = ExpenseCategory.fromId(to as? String)

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${stringResource(R.string.history_label_category)}: ",
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
                    MapChangeSection(stringResource(R.string.history_label_payers), fromMap, toMap, members, currency)
                }
                "splits" -> {
                    val fromMap = from.asSafeMap()
                    val toMap = to.asSafeMap()
                    MapChangeSection(stringResource(R.string.history_label_splits), fromMap, toMap, members, currency)
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
                            .size(16.dp)
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
                        text = stringResource(R.string.history_val_deleted),
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
                    text = if (isExpanded) stringResource(R.string.history_btn_collapse)
                    else stringResource(R.string.history_btn_show_more, significantChanges.size - 3),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

private fun formatDateHeader(timestamp: Long, todayStr: String, yesterdayStr: String): String {
    val date = Date(timestamp)
    val now = Calendar.getInstance()
    val itemDate = Calendar.getInstance().apply { time = date }

    return when {
        isSameDay(now, itemDate) -> todayStr
        isYesterday(now, itemDate) -> yesterdayStr
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