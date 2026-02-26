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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.silkfinik.fairsplit.core.domain.usecase.expense.GetExpenseHistoryScreenDataUseCase
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
        showProgress = uiState is ExpenseHistoryUiState.Loading,
        applyPadding = false
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                ExpenseHistoryUiState.Loading -> { }
                is ExpenseHistoryUiState.Error -> {
                    FairSplitEmptyState(
                        modifier = Modifier.align(Alignment.Center).padding(paddingValues),
                        icon = Icons.Default.Warning,
                        title = stringResource(R.string.group_details_error_title),
                        description = state.message.asString(context),
                        actionLabel = stringResource(R.string.action_back),
                        onActionClick = onBack
                    )
                }
                is ExpenseHistoryUiState.Success -> {
                    HistoryList(
                        historyGroups = state.historyGroups,
                        members = state.members,
                        currency = state.currency,
                        paddingValues = paddingValues
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryList(
    historyGroups: List<GetExpenseHistoryScreenDataUseCase.HistoryDateGroup>,
    members: Map<String, Member>,
    currency: Currency,
    paddingValues: PaddingValues
) {
    if (historyGroups.isEmpty()) {
        FairSplitEmptyState(
            icon = Icons.Default.History,
            title = stringResource(R.string.history_empty_title),
            description = stringResource(R.string.history_empty_desc),
            modifier = Modifier.padding(paddingValues)
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                 top = paddingValues.calculateTopPadding(),
                 bottom = paddingValues.calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            historyGroups.forEach { group ->
                stickyHeader {
                    val dateStr = when (val header = group.dateHeader) {
                        is GetExpenseHistoryScreenDataUseCase.HistoryDateHeader.Today -> stringResource(R.string.history_date_today)
                        is GetExpenseHistoryScreenDataUseCase.HistoryDateHeader.Yesterday -> stringResource(R.string.history_date_yesterday)
                        is GetExpenseHistoryScreenDataUseCase.HistoryDateHeader.SpecificDate -> header.formattedDate
                    }
                    DateHeader(dateStr)
                }

                items(
                    items = group.items,
                    key = { it.id },
                    contentType = { "HistoryItem" }
                ) { itemUI ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                    ) {
                        HistoryItemCard(item = itemUI, members = members, currency = currency)
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
    item: GetExpenseHistoryScreenDataUseCase.HistoryItemUI,
    members: Map<String, Member>,
    currency: Currency
) {
    val isCreate = item.isCreate

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
                    val timeStr = remember(item.timestamp) {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.timestamp))
                    }
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isCreate && item.createData != null) {
                CreateContent(item.createData, members, currency)
            } else if (!isCreate) {
                UpdateContent(item.updateChanges, members, currency)
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
    createData: GetExpenseHistoryScreenDataUseCase.HistoryCreateUI,
    members: Map<String, Member>,
    currency: Currency
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        createData.description?.let {
            Text(text = "${stringResource(R.string.history_label_description)}: $it", style = MaterialTheme.typography.bodyMedium)
        }
        createData.amount?.let { amount ->
            Text(
                text = "${stringResource(R.string.history_label_amount)}: ${CurrencyFormatter.format(amount, currency)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        val payers = createData.payers
        if (payers.isNotEmpty()) {
            Text("${stringResource(R.string.history_label_payers)}:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            payers.forEach { (id, amount) ->
                val memberName = members[id]?.name ?: "Unknown"
                Text(
                    text = "• $memberName: ${CurrencyFormatter.format(amount, currency)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        val splits = createData.splits
        if (splits.isNotEmpty()) {
            Text("${stringResource(R.string.history_label_splits)}:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            splits.forEach { (id, amount) ->
                val memberName = members[id]?.name ?: "Unknown"
                Text(
                    text = "• $memberName: ${CurrencyFormatter.format(amount, currency)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun UpdateContent(
    changes: List<GetExpenseHistoryScreenDataUseCase.HistoryChangeUI>,
    members: Map<String, Member>,
    currency: Currency
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        changes.forEach { change ->
            when (change) {
                is GetExpenseHistoryScreenDataUseCase.HistoryChangeUI.Amount -> {
                    val fromVal = change.from
                    val toVal = change.to

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
                is GetExpenseHistoryScreenDataUseCase.HistoryChangeUI.Description -> {
                    ChangeRow(stringResource(R.string.history_label_description), change.from, change.to)
                }
                is GetExpenseHistoryScreenDataUseCase.HistoryChangeUI.Category -> {
                    val fromCategory = ExpenseCategory.fromId(change.fromId)
                    val toCategory = ExpenseCategory.fromId(change.toId)

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
                is GetExpenseHistoryScreenDataUseCase.HistoryChangeUI.Payers -> {
                    MapChangeSection(stringResource(R.string.history_label_payers), change.from, change.to, members, currency)
                }
                is GetExpenseHistoryScreenDataUseCase.HistoryChangeUI.Splits -> {
                    MapChangeSection(stringResource(R.string.history_label_splits), change.from, change.to, members, currency)
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
    from: Map<String, Double>,
    to: Map<String, Double>,
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
            val fromVal = from[key]
            val toVal = to[key]
            fromVal != toVal
        }

        val itemsToShow = if (isExpanded) significantChanges else significantChanges.take(3)

        itemsToShow.forEach { key ->
            val fromVal = from[key]
            val toVal = to[key]
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

