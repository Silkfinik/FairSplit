package com.silkfinik.fairsplit.features.groupdetails.screen

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.Info
import kotlinx.coroutines.launch
import com.silkfinik.fairsplit.core.ui.component.CategoryIcon
import com.silkfinik.fairsplit.core.ui.component.UserAvatar
import com.silkfinik.fairsplit.core.ui.component.FairSplitTabs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.core.common.util.CurrencyFormatter
import com.silkfinik.fairsplit.core.model.Expense
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.model.Payment
import com.silkfinik.fairsplit.core.model.enums.ExpenseCategory
import com.silkfinik.fairsplit.core.model.enums.PaymentStatus
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

sealed interface TransactionItem {
    val id: String
    val date: Long
    
    data class ExpenseItem(val expense: Expense) : TransactionItem {
        override val id = expense.id
        override val date = expense.date
    }
    data class PaymentItem(val payment: Payment) : TransactionItem {
        override val id = payment.id
        override val date = payment.createdAt
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    viewModel: GroupDetailsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onAddExpenseClick: (String) -> Unit,
    onEditExpenseClick: (String, String) -> Unit,
    onMembersClick: (String) -> Unit,
    onSettleUpClick: (String, String?, String?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isGeneratingCode by viewModel.isGeneratingCode.collectAsState()
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var showInviteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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

    if (showInviteDialog) {
        val group = (uiState as? GroupDetailsUiState.Success)?.group
        if (group != null) {
            InviteDialog(
                inviteCode = group.inviteCode,
                isGenerating = isGeneratingCode,
                onGenerateCode = { viewModel.generateInviteCode() },
                onCopyCode = { code ->
                    clipboardManager.setText(AnnotatedString(code))
                },
                onDismiss = { showInviteDialog = false }
            )
        }
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
            if (uiState is GroupDetailsUiState.Success && selectedTabIndex == 0) {
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
                    Column(modifier = Modifier.fillMaxSize()) {
                        BalanceSummary(
                            balances = state.balances,
                            group = state.group,
                            currentUserId = state.currentUserId,
                            onSettleUp = { receiverId, amount -> 
                                onSettleUpClick(state.group.id, receiverId, amount) 
                            },
                            onShowDetails = { showBottomSheet = true }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        FairSplitTabs(
                            titles = listOf("Траты", "Платежи"),
                            selectedIndex = selectedTabIndex,
                            onTabSelected = { selectedTabIndex = it }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val transactions = remember(state.expenses, state.payments, selectedTabIndex) {
                            if (selectedTabIndex == 0) {
                                state.expenses.map { TransactionItem.ExpenseItem(it) }
                                    .sortedByDescending { it.date }
                            } else {
                                state.payments.map { TransactionItem.PaymentItem(it) }
                                    .sortedByDescending { it.date }
                            }
                        }

                        if (transactions.isEmpty()) {
                            FairSplitEmptyState(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                icon = if (selectedTabIndex == 0) Icons.Default.Receipt else Icons.Default.AttachMoney,
                                title = if (selectedTabIndex == 0) "Трат пока нет" else "Платежей пока нет",
                                description = if (selectedTabIndex == 0) "Добавьте первую покупку" else "Здесь будет история возвратов"
                            )
                        } else {
                            TransactionsList(
                                transactions = transactions,
                                members = state.members,
                                currentUserId = state.currentUserId,
                                group = state.group,
                                onDeleteExpense = { expenseToDelete = it },
                                onEditExpense = { expense ->
                                    onEditExpenseClick(state.group.id, expense.id)
                                },
                                onPaymentAction = { payment, status ->
                                    viewModel.updatePaymentStatus(payment.id, status)
                                }
                            )
                        }
                    }

                    if (showBottomSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showBottomSheet = false },
                            sheetState = sheetState
                        ) {
                            FullBalanceList(
                                balances = state.balances,
                                members = state.members,
                                group = state.group,
                                currentUserId = state.currentUserId,
                                onSettleUp = { receiverId, amount ->
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        if (!sheetState.isVisible) {
                                            showBottomSheet = false
                                        }
                                        onSettleUpClick(state.group.id, receiverId, amount)
                                    }
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
fun InviteDialog(
    inviteCode: String?,
    isGenerating: Boolean,
    onGenerateCode: () -> Unit,
    onCopyCode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Пригласить участников") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, 
                modifier = Modifier.fillMaxWidth()
            ) {
                if (inviteCode == null) {
                    Text(
                        "Создайте код приглашения, чтобы друзья могли присоединиться к группе.",
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isGenerating) {
                         CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        TextButton(onClick = onGenerateCode) {
                            Text("Создать код")
                        }
                    }
                } else {
                    Text("Код приглашения:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = inviteCode,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 4.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { onCopyCode(inviteCode) }) {
                        Text("Копировать")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
fun BalanceSummary(
    balances: Map<String, Double>,
    group: Group,
    currentUserId: String?,
    onSettleUp: (String?, String?) -> Unit,
    onShowDetails: () -> Unit
) {
    val myBalance = balances[currentUserId] ?: 0.0
    val activeBalances = balances.filter { abs(it.value) > 0.01 }
    val suggestedReceiverId = activeBalances.entries.filter { it.value > 0 }.maxByOrNull { it.value }?.key

    FairSplitCard(
        modifier = Modifier.padding(16.dp),
        backgroundColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Мой баланс",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (abs(myBalance) < 0.01) {
                         Text(
                            text = "Расчет окончен",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else if (myBalance > 0) {
                        Text(
                            text = "Вам должны: ${CurrencyFormatter.format(myBalance, group.currency)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF006400)
                        )
                    } else {
                        Text(
                            text = "Вы должны: ${CurrencyFormatter.format(abs(myBalance), group.currency)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB00020)
                        )
                    }
                }

                IconButton(onClick = onShowDetails) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Подробнее",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (myBalance < -0.01) {
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Button(
                    onClick = { onSettleUp(suggestedReceiverId, abs(myBalance).toString()) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Вернуть")
                }
            }
        }
    }
}

@Composable
fun TransactionsList(
    transactions: List<TransactionItem>,
    members: List<Member>,
    currentUserId: String?,
    group: Group,
    onDeleteExpense: (Expense) -> Unit,
    onEditExpense: (Expense) -> Unit,
    onPaymentAction: (Payment, PaymentStatus) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(transactions) { item ->
            when (item) {
                is TransactionItem.ExpenseItem -> {
                    ExpenseItem(
                        expense = item.expense,
                        isEditable = item.expense.creatorId == currentUserId,
                        onDelete = { onDeleteExpense(item.expense) },
                        onEdit = { onEditExpense(item.expense) },
                        onClick = { onEditExpense(item.expense) },
                        members = members
                    )
                }
                is TransactionItem.PaymentItem -> {
                    PaymentItem(
                        payment = item.payment,
                        members = members,
                        group = group,
                        currentUserId = currentUserId,
                        onAction = { status -> onPaymentAction(item.payment, status) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseItem(
    expense: Expense,
    members: List<Member>,
    isEditable: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onClick: () -> Unit
) {
    val category = ExpenseCategory.fromId(expense.category)
    val payerId = expense.payers.keys.firstOrNull() ?: expense.creatorId
    val payer = members.find { it.id == payerId }

    FairSplitCard(
        modifier = Modifier.clickable(onClick = onClick),
        backgroundColor = if (!expense.isMathValid) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!expense.isMathValid) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                CategoryIcon(category = category, size = 48.dp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = expense.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (payer != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        UserAvatar(
                            photoUrl = payer.photoUrl, 
                            name = payer.name, 
                            size = 20.dp
                        )
                    }
                }
                
                if (!expense.isMathValid) {
                    Text(
                        text = "Ошибка в расчетах",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(expense.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

@Composable
fun PaymentItem(
    payment: Payment,
    members: List<Member>,
    group: Group,
    currentUserId: String?,
    onAction: (PaymentStatus) -> Unit
) {
    FairSplitCard {
        Column(modifier = Modifier.padding(16.dp)) {
            val payer = members.find { it.id == payment.payerId }
            val receiver = members.find { it.id == payment.receiverId }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (payer != null) {
                    UserAvatar(
                        photoUrl = payer.photoUrl,
                        name = payer.name,
                        size = 48.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = Color(0xFF006400),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val payerName = payer?.name ?: "..."
                    val receiverName = receiver?.name ?: "..."
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = payerName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp).padding(horizontal = 4.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = receiverName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (receiver != null) {
                             Spacer(modifier = Modifier.width(4.dp))
                             UserAvatar(
                                photoUrl = receiver.photoUrl,
                                name = receiver.name,
                                size = 16.dp
                             )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when(payment.status) {
                            PaymentStatus.PENDING -> "Ожидает подтверждения"
                            PaymentStatus.CONFIRMED -> "Подтверждено"
                            PaymentStatus.REJECTED -> "Отклонено"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when(payment.status) {
                            PaymentStatus.PENDING -> MaterialTheme.colorScheme.primary
                            PaymentStatus.CONFIRMED -> Color(0xFF006400)
                            PaymentStatus.REJECTED -> MaterialTheme.colorScheme.error
                        }
                    )
                }
                
                Text(
                    text = CurrencyFormatter.format(payment.amount, group.currency),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF006400)
                )
            }

            if (currentUserId == payment.receiverId && payment.status == PaymentStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onAction(PaymentStatus.REJECTED) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Отклонить")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { onAction(PaymentStatus.CONFIRMED) },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF006400))
                    ) {
                        Text("Подтвердить")
                    }
                }
            }
        }
    }
}