package com.silkfinik.fairsplit.features.expenses.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.R
import com.silkfinik.fairsplit.core.model.enums.SplitType
import com.silkfinik.fairsplit.core.ui.common.ObserveAsEvents
import com.silkfinik.fairsplit.core.ui.component.FairSplitAmountInput
import com.silkfinik.fairsplit.core.ui.component.FairSplitButton
import com.silkfinik.fairsplit.core.ui.component.FairSplitCategorySelector
import com.silkfinik.fairsplit.core.ui.component.FairSplitMemberRow
import com.silkfinik.fairsplit.core.ui.component.FairSplitScaffold
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
            FairSplitTopAppBar(
                title = when {
                    uiState.isReadOnly -> stringResource(R.string.expense_title_details)
                    uiState.isEditing -> stringResource(R.string.expense_title_edit)
                    else -> stringResource(R.string.expense_title_new)
                },
                onBackClick = onBack,
                actions = {
                    if (uiState.isEditing) {
                        IconButton(onClick = onHistoryClick) {
                            Icon(Icons.Default.History, contentDescription = stringResource(R.string.expense_cd_history))
                        }
                    }
                }
            )
        },
        isLoading = uiState.isLoading
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FairSplitAmountInput(
                            amount = uiState.amount,
                            onAmountChange = viewModel::onAmountChange,
                            currency = uiState.currency,
                            readOnly = uiState.isReadOnly,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.expense_label_payer),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.payerError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(uiState.members) { member ->
                                val isSelected = member.id == uiState.payerId
                                val displayName = if (member.id == uiState.currentUserId)
                                    stringResource(R.string.expense_payer_you_format, member.name)
                                else member.name
                                val displayMember = member.copy(name = displayName)

                                FairSplitUserPill(
                                    member = displayMember,
                                    isSelected = isSelected,
                                    onClick = { if (!uiState.isReadOnly) viewModel.onPayerChange(member.id) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        FairSplitTextField(
                            value = uiState.description,
                            onValueChange = viewModel::onDescriptionChange,
                            label = stringResource(R.string.expense_label_description),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            readOnly = uiState.isReadOnly,
                            isError = uiState.descriptionError != null,
                            supportingText = uiState.descriptionError?.asString(context)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = stringResource(R.string.expense_label_category),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        FairSplitCategorySelector(
                            selectedCategory = uiState.category,
                            onCategorySelected = viewModel::onCategoryChange,
                            enabled = !uiState.isReadOnly,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.expense_label_split),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.splitError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )

                            if (!uiState.isReadOnly && uiState.splitType == SplitType.EQUAL) {
                                val allSelected = uiState.splits.size == uiState.members.size
                                TextButton(onClick = { viewModel.toggleAllMembers(!allSelected) }) {
                                    Text(if (allSelected) stringResource(R.string.expense_btn_deselect_all) else stringResource(R.string.expense_btn_select_all))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (!uiState.isReadOnly) {
                            SplitTypeSelector(
                                selectedType = uiState.splitType,
                                onTypeSelected = viewModel::onSplitTypeChange
                            )
                        } else {
                            Text(
                                text = when (uiState.splitType) {
                                    SplitType.EQUAL -> stringResource(R.string.split_type_equal)
                                    SplitType.EXACT -> stringResource(R.string.split_type_exact)
                                    SplitType.PERCENT -> stringResource(R.string.split_type_percent)
                                    SplitType.SHARES -> stringResource(R.string.split_type_shares)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (uiState.splitError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
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

                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
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
                        text = stringResource(R.string.action_save),
                        onClick = viewModel::onSaveClick,
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = uiState.isLoading
                    )
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
        SplitType.EQUAL to stringResource(R.string.split_type_equal),
        SplitType.EXACT to stringResource(R.string.split_type_exact_short),
        SplitType.PERCENT to stringResource(R.string.split_type_percent_symbol),
        SplitType.SHARES to stringResource(R.string.split_type_shares)
    )

    FairSplitTabs(
        titles = types.map { it.second },
        selectedIndex = types.indexOfFirst { it.first == selectedType },
        onTabSelected = { index -> onTypeSelected(types[index].first) }
    )
}