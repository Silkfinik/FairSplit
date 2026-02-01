package com.silkfinik.fairsplit.features.expenses.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.core.common.util.CurrencyFormatter
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.model.enums.ExpenseCategory
import com.silkfinik.fairsplit.core.model.enums.SplitType
import com.silkfinik.fairsplit.core.ui.common.ObserveAsEvents
import com.silkfinik.fairsplit.core.ui.component.FairSplitTopAppBar
import com.silkfinik.fairsplit.features.expenses.viewmodel.CreateExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExpenseScreen(
    viewModel: CreateExpenseViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onHistoryClick: () -> Unit = {}
) {
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // --- Input Fields (Description, Amount, Category, Payer) ---
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = viewModel::onDescriptionChange,
                        label = { Text("Описание") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        readOnly = uiState.isReadOnly,
                        isError = uiState.descriptionError != null,
                        supportingText = {
                            if (uiState.descriptionError != null) {
                                Text(uiState.descriptionError!!)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.amount,
                        onValueChange = viewModel::onAmountChange,
                        label = { Text("Сумма") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = uiState.isReadOnly,
                        isError = uiState.amountError != null,
                        supportingText = {
                            if (uiState.amountError != null) {
                                Text(uiState.amountError!!)
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Категория",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    CategorySelector(
                        selectedCategory = uiState.category,
                        onCategorySelected = viewModel::onCategoryChange,
                        enabled = !uiState.isReadOnly
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Кто платил",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.payerError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    if (uiState.payerError != null) {
                        Text(
                            text = uiState.payerError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    PayerDropdown(
                        members = uiState.members,
                        selectedPayerId = uiState.payerId,
                        currentUserId = uiState.currentUserId,
                        onPayerSelected = viewModel::onPayerChange,
                        isError = uiState.payerError != null,
                        enabled = !uiState.isReadOnly
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // --- Split Section ---
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "На кого делить",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.splitError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Split Type Tabs
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
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Error / Status Message
                        if (uiState.splitError != null) {
                            Text(
                                text = uiState.splitError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Select All (Only for Equal)
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
                    }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(uiState.members) { member ->
                            val displayName = if (member.id == uiState.currentUserId) "${member.name} (Вы)" else member.name
                            val isSelected = viewModel.isMemberSelected(member.id)
                            
                            SplitMemberItem(
                                name = displayName,
                                isSelected = isSelected,
                                splitType = uiState.splitType,
                                amount = uiState.splits[member.id] ?: 0.0,
                                inputValue = viewModel.getSplitValue(member.id),
                                currency = uiState.currency,
                                onToggle = { viewModel.onSplitMemberToggle(member.id) },
                                onValueChange = { viewModel.onSplitDataChange(member.id, it) },
                                enabled = !uiState.isReadOnly
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!uiState.isReadOnly) {
                        Button(
                            onClick = viewModel::onSaveClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Сохранить")
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
    
    TabRow(selectedTabIndex = types.indexOfFirst { it.first == selectedType }) {
        types.forEach { (type, title) ->
            Tab(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                text = { Text(title) }
            )
        }
    }
}

@Composable
fun CategorySelector(
    selectedCategory: ExpenseCategory,
    onCategorySelected: (ExpenseCategory) -> Unit,
    enabled: Boolean = true
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(ExpenseCategory.entries) { category ->
            FilterChip(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                label = { Text(category.displayName) },
                leadingIcon = {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                enabled = enabled,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun SplitMemberItem(
    name: String,
    isSelected: Boolean,
    splitType: SplitType,
    amount: Double,
    inputValue: String,
    currency: Currency,
    onToggle: () -> Unit,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && splitType == SplitType.EQUAL) { onToggle() }
            .padding(vertical = 4.dp)
    ) {
        if (splitType == SplitType.EQUAL) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                enabled = enabled
            )
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        
        if (splitType == SplitType.EQUAL) {
            if (isSelected && amount > 0) {
                Text(
                    text = CurrencyFormatter.format(amount, currency),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
             Row(verticalAlignment = Alignment.CenterVertically) {
                 if (amount > 0 && splitType != SplitType.EXACT) {
                     Text(
                        text = CurrencyFormatter.format(amount, currency),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                 }
                 
                 OutlinedTextField(
                     value = inputValue,
                     onValueChange = onValueChange,
                     modifier = Modifier.width(80.dp),
                     keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                     singleLine = true,
                     enabled = enabled,
                     placeholder = {
                         Text(when(splitType) {
                             SplitType.PERCENT -> "%"
                             SplitType.SHARES -> "1"
                             else -> currency.symbol
                         })
                     }
                 )
             }
        }
    }
}

@Composable
fun PayerDropdown(
    members: List<Member>,
    selectedPayerId: String?,
    currentUserId: String?,
    onPayerSelected: (String) -> Unit,
    isError: Boolean = false,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedMember = members.find { it.id == selectedPayerId }
    val selectedName = selectedMember?.let { 
        if (it.id == currentUserId) "${it.name} (Вы)" else it.name 
    } ?: "Выберите..."

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, "Выбрать")
            },
            isError = isError,
            interactionSource = remember { MutableInteractionSource() }
                .also { interactionSource ->
                    LaunchedEffect(interactionSource) {
                        interactionSource.interactions.collect {
                            if (enabled && it is PressInteraction.Release) {
                                expanded = true
                            }
                        }
                    }
                }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            members.forEach { member ->
                val displayName = if (member.id == currentUserId) "${member.name} (Вы)" else member.name
                DropdownMenuItem(
                    text = { Text(displayName) },
                    onClick = {
                        onPayerSelected(member.id)
                        expanded = false
                    }
                )
            }
        }
    }
}