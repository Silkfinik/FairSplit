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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.silkfinik.fairsplit.core.ui.component.CategoryIcon
import com.silkfinik.fairsplit.core.ui.component.FairSplitTopAppBar
import com.silkfinik.fairsplit.core.ui.component.UserAvatar
import com.silkfinik.fairsplit.core.ui.component.FairSplitCheckbox
import com.silkfinik.fairsplit.core.ui.component.FairSplitTabs
import com.silkfinik.fairsplit.core.ui.component.FairSplitTextField
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
                                PayerSelectorPill(
                                    members = uiState.members,
                                    selectedPayerId = uiState.payerId,
                                    currentUserId = uiState.currentUserId,
                                    onPayerSelected = viewModel::onPayerChange,
                                    enabled = !uiState.isReadOnly
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                FairSplitTextField(
                                    value = uiState.description,
                                    onValueChange = viewModel::onDescriptionChange,
                                    label = "Описание",
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                    readOnly = uiState.isReadOnly,
                                    isError = uiState.descriptionError != null,
                                    supportingText = uiState.descriptionError
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Категория",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                CategorySelector(
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
                                        text = uiState.splitError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        items(uiState.members) { member ->
                            val displayName = if (member.id == uiState.currentUserId) "${member.name} (Вы)" else member.name
                            val isSelected = viewModel.isMemberSelected(member.id)
                            
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                SplitMemberItem(
                                    member = member,
                                    displayName = displayName,
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

@Composable
fun CategorySelector(
    selectedCategory: ExpenseCategory,
    onCategorySelected: (ExpenseCategory) -> Unit,
    enabled: Boolean = true
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        items(ExpenseCategory.entries) { category ->
            val isSelected = category == selectedCategory
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        enabled = enabled, 
                        interactionSource = remember { MutableInteractionSource() }, 
                        indication = null
                    ) { onCategorySelected(category) }
                    .animateContentSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        ) {}
                    }
                    CategoryIcon(
                        category = category, 
                        size = if (isSelected) 64.dp else 48.dp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun SplitMemberItem(
    member: Member,
    displayName: String,
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
            .padding(vertical = 8.dp)
    ) {
        if (splitType == SplitType.EQUAL) {
            FairSplitCheckbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                enabled = enabled
            )
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        UserAvatar(
            photoUrl = member.photoUrl,
            name = member.name,
            size = 32.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = displayName,
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
                 
                 FairSplitTextField(
                     value = inputValue,
                     onValueChange = onValueChange,
                     label = "",
                     modifier = Modifier.width(80.dp),
                     keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                     singleLine = true,
                     enabled = enabled,
                     placeholder = when(splitType) {
                         SplitType.PERCENT -> "%"
                         SplitType.SHARES -> "1"
                         else -> currency.symbol
                     }
                 )
             }
        }
    }
}

@Composable
fun PayerSelectorPill(
    members: List<Member>,
    selectedPayerId: String?,
    currentUserId: String?,
    onPayerSelected: (String) -> Unit,
    enabled: Boolean = true
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        items(members) { member ->
            val isSelected = member.id == selectedPayerId
            val displayName = if (member.id == currentUserId) "${member.name} (Вы)" else member.name
            
            Surface(
                onClick = { if (enabled) onPayerSelected(member.id) },
                shape = RoundedCornerShape(50),
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                modifier = Modifier.animateContentSize()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(4.dp)
                ) {
                    UserAvatar(
                        photoUrl = member.photoUrl,
                        name = member.name,
                        size = 32.dp
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }
        }
    }
}