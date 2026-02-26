package com.silkfinik.fairsplit.features.groupdetails.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.R
import com.silkfinik.fairsplit.core.model.Expense
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.model.Payment
import com.silkfinik.fairsplit.core.model.enums.ExpenseCategory
import com.silkfinik.fairsplit.core.model.enums.PaymentStatus
import com.silkfinik.fairsplit.core.ui.common.ObserveAsEvents
import com.silkfinik.fairsplit.core.ui.component.CategoryIcon
import com.silkfinik.fairsplit.core.ui.component.FairSplitBottomSheet
import com.silkfinik.fairsplit.core.ui.component.FairSplitButton
import com.silkfinik.fairsplit.core.ui.component.FairSplitButtonStyle
import com.silkfinik.fairsplit.core.ui.component.FairSplitCard
import com.silkfinik.fairsplit.core.ui.component.FairSplitEmptyState
import com.silkfinik.fairsplit.core.ui.component.FairSplitMoneyText
import com.silkfinik.fairsplit.core.ui.component.FairSplitScaffold
import com.silkfinik.fairsplit.core.ui.component.FairSplitTabs
import com.silkfinik.fairsplit.core.ui.component.FairSplitTextField
import com.silkfinik.fairsplit.core.ui.component.FairSplitTopAppBar
import com.silkfinik.fairsplit.core.ui.component.FairSplitUserAvatar
import com.silkfinik.fairsplit.features.groupdetails.ui.GroupDetailsUiState
import com.silkfinik.fairsplit.features.groupdetails.viewmodel.GroupDetailsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class BottomSheetType {
    BALANCES, EDIT_GROUP
}

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GroupDetailsScreen(
    viewModel: GroupDetailsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onAddExpenseClick: (String) -> Unit,
    onEditExpenseClick: (String, String) -> Unit,
    onMembersClick: (String) -> Unit,
    onSettleUpClick: (String, String?, String?) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var bottomSheetType by remember { mutableStateOf(BottomSheetType.BALANCES) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
            title = { Text(stringResource(R.string.group_dialog_delete_expense_title)) },
            text = { Text(stringResource(R.string.group_dialog_delete_expense_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        expenseToDelete?.let { viewModel.deleteExpense(it.id) }
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.group_dialog_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge
        )
    }

    FairSplitScaffold(
        snackbarHostState = snackbarHostState,
        topBar = {
            FairSplitTopAppBar(
                titleContent = {
                    if (uiState is GroupDetailsUiState.Success) {
                        val group = (uiState as GroupDetailsUiState.Success).group
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    bottomSheetType = BottomSheetType.EDIT_GROUP
                                    showBottomSheet = true
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            FairSplitUserAvatar(
                                photoUrl = group.avatarUrl,
                                name = group.name,
                                size = 28.dp,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.group_details_default_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                onBackClick = onBackClick,
                actions = {
                    if (uiState is GroupDetailsUiState.Success) {
                        IconButton(onClick = { onMembersClick((uiState as GroupDetailsUiState.Success).group.id) }) {
                            Icon(Icons.Default.Group, stringResource(R.string.group_details_cd_members))
                        }
                    }
                }
            )
        },
        isLoading = uiState is GroupDetailsUiState.Loading
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                GroupDetailsUiState.Loading -> { }
                is GroupDetailsUiState.Error -> {
                    FairSplitEmptyState(
                        modifier = Modifier.align(Alignment.Center),
                        icon = Icons.Default.Warning,
                        title = stringResource(R.string.group_details_error_title),
                        description = state.message.asString(context),
                        actionLabel = stringResource(R.string.action_back),
                        onActionClick = onBackClick
                    )
                }
                is GroupDetailsUiState.Success -> {
                    val transactions = remember(state.expenses, state.payments, selectedTabIndex) {
                        if (selectedTabIndex == 0) {
                            state.expenses.map { TransactionItem.ExpenseItem(it) }
                                .sortedByDescending { it.date }
                        } else {
                            state.payments.map { TransactionItem.PaymentItem(it) }
                                .sortedByDescending { it.date }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                BalanceSummary(
                                    balances = state.balances,
                                    group = state.group,
                                    currentUserId = state.currentUserId,
                                    onSettleUp = { receiverId, amount ->
                                        onSettleUpClick(state.group.id, receiverId, amount)
                                    },
                                    onShowDetails = { 
                                        bottomSheetType = BottomSheetType.BALANCES
                                        showBottomSheet = true 
                                    }
                                )
                            }
                        }

                        stickyHeader {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(bottom = 8.dp)
                            ) {
                                FairSplitTabs(
                                    titles = listOf(
                                        stringResource(R.string.group_tab_expenses),
                                        stringResource(R.string.group_tab_payments)
                                    ),
                                    selectedIndex = selectedTabIndex,
                                    onTabSelected = { selectedTabIndex = it }
                                )
                            }
                        }

                        if (transactions.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FairSplitEmptyState(
                                        icon = if (selectedTabIndex == 0) Icons.Default.Receipt else Icons.Default.AttachMoney,
                                        title = if (selectedTabIndex == 0) stringResource(R.string.group_empty_expenses_title) else stringResource(R.string.group_empty_payments_title),
                                        description = if (selectedTabIndex == 0) stringResource(R.string.group_empty_expenses_desc) else stringResource(R.string.group_empty_payments_desc)
                                    )
                                }
                            }
                        } else {
                            items(transactions, key = { it.id }) { item ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                    when (item) {
                                        is TransactionItem.ExpenseItem -> {
                                            ExpenseItem(
                                                expense = item.expense,
                                                isEditable = item.expense.creatorId == state.currentUserId,
                                                onDelete = { expenseToDelete = item.expense },
                                                onEdit = { onEditExpenseClick(state.group.id, item.expense.id) },
                                                onClick = { onEditExpenseClick(state.group.id, item.expense.id) },
                                                members = state.members
                                            )
                                        }
                                        is TransactionItem.PaymentItem -> {
                                            PaymentItem(
                                                payment = item.payment,
                                                members = state.members,
                                                group = state.group,
                                                currentUserId = state.currentUserId,
                                                onAction = { status ->
                                                    viewModel.updatePaymentStatus(item.payment.id, status)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        FairSplitButton(
                            text = stringResource(R.string.group_btn_add_expense),
                            onClick = { onAddExpenseClick(state.group.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (showBottomSheet) {
                        FairSplitBottomSheet(
                            onDismissRequest = { showBottomSheet = false },
                            sheetState = sheetState
                        ) {
                            when (bottomSheetType) {
                                BottomSheetType.BALANCES -> {
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
                                BottomSheetType.EDIT_GROUP -> {
                                    EditGroupSheet(
                                        group = state.group,
                                        isUpdating = viewModel.isUpdatingGroup.collectAsStateWithLifecycle().value,
                                        onSave = { newName, newAvatarUri ->
                                            viewModel.updateGroupDetails(newName, newAvatarUri)
                                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                                if (!sheetState.isVisible) {
                                                    showBottomSheet = false
                                                }
                                            }
                                        },
                                        onCancel = {
                                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                                if (!sheetState.isVisible) {
                                                    showBottomSheet = false
                                                }
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
    }
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
        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
        onClick = onShowDetails
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.balance_summary_my_balance),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FairSplitMoneyText(
                        amount = myBalance,
                        currency = group.currency,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        useColor = true,
                        showSign = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (abs(myBalance) < 0.01) stringResource(R.string.balance_summary_status_settled)
                        else if (myBalance > 0) stringResource(R.string.balance_summary_status_owed)
                        else stringResource(R.string.balance_summary_status_debt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                )
            }

            if (myBalance < -0.01) {
                Spacer(modifier = Modifier.height(20.dp))
                FairSplitButton(
                    text = stringResource(R.string.balance_summary_btn_settle),
                    onClick = { onSettleUp(suggestedReceiverId, abs(myBalance).toString()) },
                    style = FairSplitButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth()
                )
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

    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = MaterialTheme.colorScheme.errorContainer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, MaterialTheme.shapes.extraLarge)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.group_dialog_delete_confirm),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = isEditable
    ) {
        FairSplitCard(onClick = onClick) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    CategoryIcon(category = category, size = 48.dp)
                    if (!expense.isMathValid) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(R.string.transaction_expense_error),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.TopEnd)
                                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (payer != null) {
                            FairSplitUserAvatar(
                                photoUrl = payer.photoUrl,
                                name = payer.name,
                                size = 16.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = payer.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    FairSplitMoneyText(
                        amount = expense.amount,
                        currency = expense.currency,
                        style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary),
                        useColor = false,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(expense.date)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
    val payer = members.find { it.id == payment.payerId }
    val receiver = members.find { it.id == payment.receiverId }
    val isIncoming = currentUserId == payment.receiverId

    FairSplitCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    FairSplitUserAvatar(
                        photoUrl = payer?.photoUrl,
                        name = payer?.name ?: "?",
                        size = 40.dp
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(16.dp)
                )

                FairSplitUserAvatar(
                    photoUrl = receiver?.photoUrl,
                    name = receiver?.name ?: "?",
                    size = 40.dp
                )

                Spacer(modifier = Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.End) {
                    FairSplitMoneyText(
                        amount = payment.amount,
                        currency = group.currency,
                        style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF1B5E20)),
                        useColor = false,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val statusColor = when (payment.status) {
                    PaymentStatus.PENDING -> MaterialTheme.colorScheme.primary
                    PaymentStatus.CONFIRMED -> Color(0xFF1B5E20)
                    PaymentStatus.REJECTED -> MaterialTheme.colorScheme.error
                }

                val statusText = when (payment.status) {
                    PaymentStatus.PENDING -> stringResource(R.string.payment_status_pending)
                    PaymentStatus.CONFIRMED -> stringResource(R.string.payment_status_confirmed)
                    PaymentStatus.REJECTED -> stringResource(R.string.payment_status_rejected)
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )

                if (isIncoming && payment.status == PaymentStatus.PENDING) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { onAction(PaymentStatus.REJECTED) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.payment_action_reject))
                        }
                        TextButton(
                            onClick = { onAction(PaymentStatus.CONFIRMED) },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1B5E20))
                        ) {
                            Text(stringResource(R.string.payment_action_accept))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditGroupSheet(
    group: Group,
    isUpdating: Boolean,
    onSave: (String, String?) -> Unit,
    onCancel: () -> Unit
) {
    var editedName by remember { mutableStateOf(group.name) }
    var selectedAvatarUri by remember { mutableStateOf<String?>(group.avatarUrl) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { selectedAvatarUri = it.toString() } }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.group_details_edit_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier.clickable {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        ) {
            FairSplitUserAvatar(
                photoUrl = selectedAvatarUri,
                name = editedName.ifBlank { "?" },
                size = 100.dp,
                fontSize = 40.sp,
                modifier = Modifier.clip(CircleShape)
            )
            
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(32.dp)
                    .offset(x = 4.dp, y = 4.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        FairSplitTextField(
            value = editedName,
            onValueChange = { editedName = it },
            label = stringResource(R.string.create_group_label_name),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FairSplitButton(
                text = stringResource(R.string.action_cancel),
                onClick = onCancel,
                style = FairSplitButtonStyle.Secondary,
                modifier = Modifier.weight(1f)
            )
            FairSplitButton(
                text = stringResource(R.string.action_save),
                onClick = { onSave(editedName, selectedAvatarUri) },
                enabled = editedName.isNotBlank() && !isUpdating,
                isLoading = isUpdating,
                modifier = Modifier.weight(1f)
            )
        }
    }
}