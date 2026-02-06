package com.silkfinik.fairsplit.features.expenses.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.R
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.common.util.UiText
import com.silkfinik.fairsplit.core.common.util.asUiText
import com.silkfinik.fairsplit.core.common.util.onError
import com.silkfinik.fairsplit.core.common.util.onSuccess
import com.silkfinik.fairsplit.core.domain.usecase.expense.PrepareCreateExpenseUseCase
import com.silkfinik.fairsplit.core.domain.usecase.expense.SaveExpenseUseCase
import com.silkfinik.fairsplit.core.model.enums.ExpenseCategory
import com.silkfinik.fairsplit.core.model.enums.SplitType
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import com.silkfinik.fairsplit.features.expenses.ui.CreateExpenseUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class CreateExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val prepareCreateExpenseUseCase: PrepareCreateExpenseUseCase,
    private val saveExpenseUseCase: SaveExpenseUseCase
) : BaseViewModel() {

    private val groupId: String = savedStateHandle["groupId"] ?: ""
    private val expenseId: String? = savedStateHandle["expenseId"]

    private val _uiState = MutableStateFlow(CreateExpenseUiState())
    val uiState: StateFlow<CreateExpenseUiState> = _uiState.asStateFlow()

    init {
        if (groupId.isBlank()) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.error_invalid_group_id)) }
        } else {
            loadData()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            prepareCreateExpenseUseCase(groupId, expenseId)
                .onSuccess { data ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currency = data.currency,
                            members = data.members,
                            currentUserId = data.currentUserId,
                            isEditing = data.isEditing,
                            isReadOnly = data.isReadOnly,
                            description = data.description,
                            amount = data.amount,
                            category = data.category,
                            payerId = data.payerId,
                            splitType = data.splitType,
                            splits = data.splits,
                            splitData = data.splitData,
                            selectedSplitMemberIds = data.selectedMemberIds
                        )
                    }
                    if (!data.isEditing) {
                        recalculateSplits()
                    }
                }
                .onError { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.asUiText()) }
                }
        }
    }

    fun onDescriptionChange(description: String) {
        val error = if (description.isBlank()) UiText.StringResource(R.string.error_enter_description) else null
        _uiState.update { it.copy(description = description, descriptionError = error) }
    }

    fun onAmountChange(amount: String) {
        val doubleVal = amount.toDoubleOrNull()
        val error = if (amount.isNotBlank() && (doubleVal == null || doubleVal <= 0)) {
            UiText.StringResource(R.string.error_invalid_amount)
        } else null

        _uiState.update { it.copy(amount = amount, amountError = error) }
        recalculateSplits()
    }

    fun onCategoryChange(category: ExpenseCategory) {
        _uiState.update { it.copy(category = category) }
    }

    fun onPayerChange(payerId: String) {
        _uiState.update { it.copy(payerId = payerId, payerError = null) }
    }

    fun onSplitTypeChange(type: SplitType) {
        _uiState.update { it.copy(splitType = type, splitError = null) }
        recalculateSplits()
    }

    fun onSplitDataChange(memberId: String, value: String) {
        val doubleValue = value.toDoubleOrNull()

        val currentSplitData = _uiState.value.splitData.toMutableMap()
        if (doubleValue != null && doubleValue >= 0) {
            currentSplitData[memberId] = doubleValue
        } else {
            currentSplitData.remove(memberId)
        }

        val currentSelected = _uiState.value.selectedSplitMemberIds.toMutableSet()
        if (value.isNotBlank() && doubleValue != null && doubleValue > 0) {
            currentSelected.add(memberId)
        } else {
            currentSelected.remove(memberId)
        }

        _uiState.update { it.copy(splitData = currentSplitData, selectedSplitMemberIds = currentSelected) }
        recalculateSplits()
    }

    fun onSplitMemberToggle(memberId: String) {
        val currentIds = _uiState.value.selectedSplitMemberIds.toMutableSet()
        if (currentIds.contains(memberId)) {
            currentIds.remove(memberId)
        } else {
            currentIds.add(memberId)
        }

        _uiState.update { it.copy(selectedSplitMemberIds = currentIds) }
        recalculateSplits()
    }

    fun toggleAllMembers(selectAll: Boolean) {
        val allIds = if (selectAll) {
            _uiState.value.members.map { it.id }.toSet()
        } else {
            emptySet()
        }
        _uiState.update { it.copy(selectedSplitMemberIds = allIds) }
        recalculateSplits()
    }

    private fun recalculateSplits() {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull() ?: 0.0
        val selectedIds = state.selectedSplitMemberIds
        val splitData = state.splitData

        var newSplits = emptyMap<String, Double>()
        var error: UiText? = null

        if (amount <= 0) {
            _uiState.update { it.copy(splits = emptyMap(), splitError = null) }
            return
        }

        when (state.splitType) {
            SplitType.EQUAL -> {
                val count = selectedIds.size
                if (count > 0) {
                    val splitAmount = amount / count
                    newSplits = selectedIds.associateWith { splitAmount }
                } else {
                    error = UiText.StringResource(R.string.error_select_at_least_one)
                }
            }
            SplitType.EXACT -> {
                val currentSum = splitData.values.sum()
                newSplits = splitData.filterValues { it > 0 }

                if (abs(amount - currentSum) > 0.02) {
                    val diff = amount - currentSum
                    val diffStr = String.format("%.2f", abs(diff))
                    error = if (diff > 0) {
                        UiText.StringResource(R.string.error_split_remaining, diffStr)
                    } else {
                        UiText.StringResource(R.string.error_split_overflow, diffStr)
                    }
                }
            }
            SplitType.PERCENT -> {
                val currentPercentSum = splitData.values.sum()
                if (abs(100.0 - currentPercentSum) > 0.01) {
                    val diff = 100.0 - currentPercentSum
                    val diffStr = String.format("%.2f", abs(diff))
                    error = if (diff > 0) {
                        UiText.StringResource(R.string.error_split_remaining_percent, diffStr)
                    } else {
                        UiText.StringResource(R.string.error_split_overflow_percent, diffStr)
                    }
                }

                newSplits = splitData.filterValues { it > 0 }.mapValues { (_, percent) ->
                    amount * (percent / 100.0)
                }
            }
            SplitType.SHARES -> {
                val totalShares = splitData.values.sum()
                if (totalShares > 0) {
                    newSplits = splitData.filterValues { it > 0 }.mapValues { (_, share) ->
                        amount * (share / totalShares)
                    }
                } else {
                    error = UiText.StringResource(R.string.error_enter_shares)
                }
            }
        }

        _uiState.update { it.copy(splits = newSplits, splitError = error) }
    }

    fun onSaveClick() {
        val currentState = _uiState.value

        val descriptionError = if (currentState.description.isBlank()) UiText.StringResource(R.string.error_enter_description) else null
        val amountVal = currentState.amount.toDoubleOrNull()
        val amountError = if (amountVal == null || amountVal <= 0) UiText.StringResource(R.string.error_enter_amount) else null
        val payerError = if (currentState.payerId == null) UiText.StringResource(R.string.error_select_payer) else null

        recalculateSplits()
        val updatedState = _uiState.value
        val splitError = updatedState.splitError

        val totalSplit = updatedState.splits.values.sum()
        val difference = if (amountVal != null) abs(amountVal - totalSplit) else 0.0
        val balanceError = if (difference > 0.02) UiText.StringResource(R.string.error_split_mismatch) else null

        if (descriptionError != null || amountError != null || payerError != null || splitError != null || balanceError != null) {
            _uiState.update {
                it.copy(
                    descriptionError = descriptionError,
                    amountError = amountError,
                    payerError = payerError,
                    splitError = splitError ?: balanceError
                )
            }
            return
        }

        val amount = amountVal!!

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val params = SaveExpenseUseCase.Params(
                groupId = groupId,
                expenseId = expenseId,
                description = currentState.description,
                amount = amount,
                category = currentState.category.id,
                payerId = currentState.payerId!!,
                splits = currentState.splits,
                splitType = currentState.splitType,
                splitData = currentState.splitData
            )

            saveExpenseUseCase(params)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                    sendEvent(UiEvent.NavigateBack)
                }
                .onError { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowError(error.asUiText()))
                }
        }
    }

    fun isMemberSelected(memberId: String): Boolean {
        return _uiState.value.selectedSplitMemberIds.contains(memberId)
    }

    fun getSplitValue(memberId: String): String {
        return _uiState.value.splitData[memberId]?.toString()?.removeSuffix(".0") ?: ""
    }
}