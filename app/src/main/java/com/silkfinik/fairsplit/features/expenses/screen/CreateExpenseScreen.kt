package com.silkfinik.fairsplit.features.expenses.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.core.model.enums.SplitType
import com.silkfinik.fairsplit.core.ui.common.ObserveAsEvents
import com.silkfinik.fairsplit.core.ui.component.FairSplitCategorySelector
import com.silkfinik.fairsplit.core.ui.component.FairSplitMemberRow
import com.silkfinik.fairsplit.core.ui.component.FairSplitTabs
import com.silkfinik.fairsplit.core.ui.component.FairSplitTextField
import com.silkfinik.fairsplit.core.ui.component.FairSplitTopAppBar
import com.silkfinik.fairsplit.core.ui.component.FairSplitUserPill
import com.silkfinik.fairsplit.features.expenses.viewmodel.CreateExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExpenseScreen(
    viewModel: CreateExpenseViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onHistoryClick: () -> Unit = {}
) {

    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveAsEvents(
        flow = viewModel.uiEvent,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onBack
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            FairSplitTopAppBar(
                title = when {
                    uiState.isReadOnly -> "Детали траты"
                    uiState.isEditing -> "Редактирование траты"
                    else -> "Новая трата"
                },
                onBackClick = onBack,
                actions = {
                    if (uiState.isEditing) {
                        IconButton(onClick = onHistoryClick) {
                            Icon(Icons.Default.History, contentDescription = "История")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                BigAmountInput(
                                    amount = uiState.amount,
                                    onAmountChange = viewModel::onAmountChange,
                                    currency = uiState.currency,
                                    readOnly = uiState.isReadOnly
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Кто платил",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.payerError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    items(uiState.members) { member ->
                                        val isSelected = member.id == uiState.payerId
                                        val displayName = if (member.id == uiState.currentUserId) "${member.name} (Вы)" else member.name
                                        val displayMember = member.copy(name = displayName)

                                        FairSplitUserPill(
                                            member = displayMember,
                                            isSelected = isSelected,
                                            onClick = { if (!uiState.isReadOnly) viewModel.onPayerChange(member.id) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                FairSplitTextField(
                                    value = uiState.description,
                                    onValueChange = viewModel::onDescriptionChange,
                                    label = "Описание",
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                    readOnly = uiState.isReadOnly,
                                    isError = uiState.descriptionError != null,
                                    supportingText = uiState.descriptionError?.asString(context)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Категория",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                FairSplitCategorySelector(
                                    selectedCategory = uiState.category,
                                    onCategorySelected = viewModel::onCategoryChange,
                                    enabled = !uiState.isReadOnly
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Text(
                                    text = "На кого делить",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.splitError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                if (!uiState.isReadOnly) {
                                    SplitTypeSelector(
                                        selectedType = uiState.splitType,
                                        onTypeSelected = viewModel::onSplitTypeChange
                                    )
                                } else {
                                    Text(
                                        text = when (uiState.splitType) {
                                            SplitType.EQUAL -> "Поровну"
                                            SplitType.EXACT -> "Точные суммы"
                                            SplitType.PERCENT -> "Проценты"
                                            SplitType.SHARES -> "Доли"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (!uiState.isReadOnly && uiState.splitType == SplitType.EQUAL) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        val allSelected = uiState.splits.size == uiState.members.size
                                        TextButton(onClick = { viewModel.toggleAllMembers(!allSelected) }) {
                                            Text(if (allSelected) "Снять все" else "Выбрать все")
                                        }
                                    }
                                }

                                if (uiState.splitError != null) {
                                    Text(
                                        text = uiState.splitError!!.asString(context),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        items(uiState.members) { member ->
                            val isSelected = viewModel.isMemberSelected(member.id)

                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                FairSplitMemberRow(
                                    member = member,
                                    isSelected = isSelected,
                                    splitType = uiState.splitType,
                                    amount = uiState.splits[member.id] ?: 0.0,
                                    inputValue = viewModel.getSplitValue(member.id),
                                    currency = uiState.currency,
                                    onToggle = { viewModel.onSplitMemberToggle(member.id) },
                                    onValueChange = { viewModel.onSplitDataChange(member.id, it) },
                                    enabled = !uiState.isReadOnly,
                                    isCurrentUser = member.id == uiState.currentUserId
                                )
                            }
                        }
                    }

                    if (!uiState.isReadOnly) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                            shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                Button(
                                    onClick = viewModel::onSaveClick,
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Text("Сохранить")
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
fun SplitTypeSelector(
    selectedType: SplitType,
    onTypeSelected: (SplitType) -> Unit
) {
    val types = listOf(
        SplitType.EQUAL to "Поровну",
        SplitType.EXACT to "Сумма",
        SplitType.PERCENT to "%",
        SplitType.SHARES to "Доли"
    )

    FairSplitTabs(
        titles = types.map { it.second },
        selectedIndex = types.indexOfFirst { it.first == selectedType },
        onTabSelected = { index -> onTypeSelected(types[index].first) },
        modifier = Modifier.padding(horizontal = 0.dp)
    )
}