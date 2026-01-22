package com.silkfinik.fairsplit.features.groupdetails.screen

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.core.common.util.CurrencyFormatter
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.model.Expense
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.ui.common.ObserveAsEvents
import com.silkfinik.fairsplit.core.ui.component.FairSplitCard
import com.silkfinik.fairsplit.core.ui.component.FairSplitEmptyState
import com.silkfinik.fairsplit.core.ui.component.FairSplitTopAppBar
import com.silkfinik.fairsplit.features.groupdetails.ui.GroupDetailsUiState
import com.silkfinik.fairsplit.features.groupdetails.viewmodel.GroupDetailsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun GroupDetailsScreen(
    viewModel: GroupDetailsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onAddExpenseClick: (String) -> Unit,
    onEditExpenseClick: (String, String) -> Unit,
    onMembersClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveAsEvents(
        flow = viewModel.uiEvent,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onBackClick
    )

    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Удалить трату?") },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        expenseToDelete?.let { viewModel.deleteExpense(it.id) }
                        expenseToDelete = null
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val title = if (uiState is GroupDetailsUiState.Success) {
                (uiState as GroupDetailsUiState.Success).group.name
            } else {
                "Группа"
            }
            FairSplitTopAppBar(
                title = title,
                onBackClick = onBackClick,
                actions = {
                    if (uiState is GroupDetailsUiState.Success) {
                        IconButton(onClick = { onMembersClick((uiState as GroupDetailsUiState.Success).group.id) }) {
                            Icon(Icons.Default.Group, "Участники")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState is GroupDetailsUiState.Success) {
                FloatingActionButton(
                    onClick = {
                        onAddExpenseClick((uiState as GroupDetailsUiState.Success).group.id)
                    }
                ) {
                    Icon(Icons.Default.Add, "Добавить трату")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                GroupDetailsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is GroupDetailsUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                is GroupDetailsUiState.Success -> {
                    if (state.expenses.isEmpty()) {
                        FairSplitEmptyState(
                            modifier = Modifier.align(Alignment.Center),
                            icon = Icons.Default.Receipt,
                            title = "Трат пока нет",
                            description = "Добавьте первую покупку, чтобы разделить расходы"
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Balance Header
                            BalanceSummary(
                                balances = state.balances,
                                members = state.members,
                                group = state.group
                            )
                            
                            // Expenses List
                            ExpensesList(
                                expenses = state.expenses,
                                currentUserId = state.currentUserId,
                                onDeleteExpense = { expenseToDelete = it },
                                onEditExpense = { expense ->
                                    onEditExpenseClick(state.group.id, expense.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceSummary(
    balances: Map<String, Double>,
    members: List<Member>,
    group: Group
) {
    // Only show if there are non-zero balances
    val activeBalances = balances.filter { abs(it.value) > 0.01 }
    
    if (activeBalances.isNotEmpty()) {
        FairSplitCard(
            modifier = Modifier.padding(16.dp),
            backgroundColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Баланс",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                activeBalances.forEach { (memberId, balance) ->
                    val memberName = members.find { it.id == memberId }?.name ?: "Неизвестный"
                    val isCreditor = balance > 0
                    val amountText = CurrencyFormatter.format(abs(balance), group.currency)
                    val text = if (isCreditor) {
                        "$memberName должны вернуть $amountText"
                    } else {
                        "$memberName должен $amountText"
                    }
                    val textColor = if (isCreditor) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                         // A bit reddish for debt, but still readable on container
                        Color(0xFFB00020) 
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = memberName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (isCreditor) "+$amountText" else "-$amountText",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCreditor) Color(0xFF006400) else Color(0xFFB00020)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpensesList(
    expenses: List<Expense>,
    currentUserId: String?,
    onDeleteExpense: (Expense) -> Unit,
    onEditExpense: (Expense) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(expenses) { expense ->
            ExpenseItem(
                expense = expense,
                isEditable = expense.creatorId == currentUserId,
                onDelete = { onDeleteExpense(expense) },
                onEdit = { onEditExpense(expense) }
            )
        }
    }
}

@Composable
fun ExpenseItem(
    expense: Expense,
    isEditable: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    FairSplitCard {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(expense.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = CurrencyFormatter.format(expense.amount, expense.currency),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (isEditable) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Редактировать",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}