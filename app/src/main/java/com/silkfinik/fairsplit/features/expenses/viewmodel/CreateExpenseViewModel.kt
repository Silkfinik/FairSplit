package com.silkfinik.fairsplit.features.expenses.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.onError
import com.silkfinik.fairsplit.core.common.util.onSuccess
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.usecase.expense.GetExpenseUseCase
import com.silkfinik.fairsplit.core.domain.usecase.expense.SaveExpenseUseCase
import com.silkfinik.fairsplit.core.domain.usecase.group.GetGroupUseCase
import com.silkfinik.fairsplit.core.domain.usecase.member.GetMembersUseCase
import com.silkfinik.fairsplit.core.model.enums.ExpenseCategory
import com.silkfinik.fairsplit.core.model.enums.SplitType
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import com.silkfinik.fairsplit.features.expenses.ui.CreateExpenseUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.round

@HiltViewModel
class CreateExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getGroupUseCase: GetGroupUseCase,
    private val getMembersUseCase: GetMembersUseCase,
    private val getExpenseUseCase: GetExpenseUseCase,
    private val saveExpenseUseCase: SaveExpenseUseCase,
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val groupId: String = savedStateHandle["groupId"] ?: ""
    private val expenseId: String? = savedStateHandle["expenseId"]

    private val _uiState = MutableStateFlow(CreateExpenseUiState())
    val uiState: StateFlow<CreateExpenseUiState> = _uiState.asStateFlow()

    init {
        if (groupId.isBlank()) {
            _uiState.update { it.copy(error = "Некорректный ID группы") }
        } else {
            loadData()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val group = getGroupUseCase(groupId).first() ?: throw Exception("Группа не найдена")
                val allMembers = getMembersUseCase(groupId).first()
                val redirectMap = allMembers.filter { it.mergedWithUid != null }.associate { it.id to it.mergedWithUid!! }
                var displayMembers = allMembers.filter { it.mergedWithUid == null }
                val currentUserId = authRepository.getUserId()

                if (expenseId != null) {
                    val expense = getExpenseUseCase(expenseId).first()
                    if (expense != null) {
                        val originalPayerId = expense.payers.keys.firstOrNull()
                        val payerId = redirectMap[originalPayerId] ?: originalPayerId

                        // Merge splits logic: Sum amounts if multiple original members map to the same target user
                        val resolvedSplits = mutableMapOf<String, Double>()
                        expense.splits.forEach { (originalId, amount) ->
                            val targetId = redirectMap[originalId] ?: originalId
                            val currentAmount = resolvedSplits[targetId] ?: 0.0
                            resolvedSplits[targetId] = currentAmount + amount
                        }
                        
                        // Merge splitData logic
                        val resolvedSplitData = mutableMapOf<String, Double>()
                        expense.splitData.forEach { (originalId, value) ->
                            val targetId = redirectMap[originalId] ?: originalId
                            val currentValue = resolvedSplitData[targetId] ?: 0.0
                            resolvedSplitData[targetId] = currentValue + value
                        }

                        // Collect names of ghosts involved in this expense
                        val redirectedNamesMap = mutableMapOf<String, MutableSet<String>>()
                        
                        // Check payer redirect
                        if (originalPayerId != null && redirectMap.containsKey(originalPayerId)) {
                            val targetId = redirectMap[originalPayerId]!!
                            allMembers.find { it.id == originalPayerId }?.name?.let { 
                                redirectedNamesMap.getOrPut(targetId) { mutableSetOf() }.add(it)
                            }
                        }
                        
                        // Check splits redirect
                        expense.splits.keys.forEach { originalId ->
                            if (redirectMap.containsKey(originalId)) {
                                val targetId = redirectMap[originalId]!!
                                allMembers.find { it.id == originalId }?.name?.let {
                                    redirectedNamesMap.getOrPut(targetId) { mutableSetOf() }.add(it)
                                }
                            }
                        }
                        
                        // Rename members
                        displayMembers = displayMembers.map { member ->
                            val ghostNames = redirectedNamesMap[member.id]
                            if (!ghostNames.isNullOrEmpty()) {
                                member.copy(name = "${member.name} (${ghostNames.joinToString(", ")})")
                            } else {
                                member
                            }
                        }
                        
                        val initialSelected = if (expense.splitType == SplitType.EQUAL) {
                            resolvedSplits.keys
                        } else {
                             resolvedSplitData.keys.ifEmpty { resolvedSplits.keys }
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isEditing = true,
                                isReadOnly = expense.creatorId != currentUserId,
                                currency = group.currency,
                                members = displayMembers,
                                description = expense.description,
                                amount = expense.amount.toString(),
                                category = ExpenseCategory.fromId(expense.category),
                                payerId = payerId,
                                currentUserId = currentUserId,
                                splits = resolvedSplits,
                                splitType = expense.splitType,
                                splitData = resolvedSplitData,
                                selectedSplitMemberIds = initialSelected
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Трата не найдена",
                                members = displayMembers,
                                currency = group.currency,
                                currentUserId = currentUserId
                            )
                        }
                    }
                } else {
                    val allMemberIds = displayMembers.map { it.id }.toSet()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currency = group.currency,
                            members = displayMembers,
                            currentUserId = currentUserId,
                            payerId = it.payerId ?: displayMembers.firstOrNull()?.id,
                            selectedSplitMemberIds = allMemberIds
                        )
                    }
                    recalculateSplits()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onDescriptionChange(description: String) {
        val error = if (description.isBlank()) "Введите описание" else null
        _uiState.update { it.copy(description = description, descriptionError = error) }
    }

    fun onAmountChange(amount: String) {
        val doubleVal = amount.toDoubleOrNull()
        val error = if (amount.isNotBlank() && (doubleVal == null || doubleVal <= 0)) "Некорректная сумма" else null
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
        
        // Update selected members based on data input for non-equal splits
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
        // Only for EQUAL split or just toggling selection state
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
        var error: String? = null

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
                    error = "Выберите хотя бы одного"
                }
            }
            SplitType.EXACT -> {
                // Sum of entered amounts
                val currentSum = splitData.values.sum()
                newSplits = splitData.filterValues { it > 0 }
                
                if (abs(amount - currentSum) > 0.02) {
                     val diff = amount - currentSum
                     val diffStr = String.format("%.2f", abs(diff))
                     error = if (diff > 0) "Осталось распределить: $diffStr" else "Перебор: $diffStr"
                }
            }
            SplitType.PERCENT -> {
                 val currentPercentSum = splitData.values.sum()
                 if (abs(100.0 - currentPercentSum) > 0.01) {
                     val diff = 100.0 - currentPercentSum
                     val diffStr = String.format("%.2f", abs(diff))
                     error = if (diff > 0) "Осталось: $diffStr%" else "Перебор: $diffStr%"
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
                    error = "Введите доли"
                }
            }
        }

        _uiState.update { it.copy(splits = newSplits, splitError = error) }
    }

    fun onSaveClick() {
        val currentState = _uiState.value

        val descriptionError = if (currentState.description.isBlank()) "Введите описание" else null
        val amountVal = currentState.amount.toDoubleOrNull()
        val amountError = if (amountVal == null || amountVal <= 0) "Введите сумму" else null
        val payerError = if (currentState.payerId == null) "Выберите плательщика" else null
        
        // Re-validate logic based on recalculateSplits
        recalculateSplits() // Ensure state is fresh
        val updatedState = _uiState.value
        val splitError = updatedState.splitError

        // Extra check for split sum matching total (mostly for EXACT mode, but good safety net)
        val totalSplit = updatedState.splits.values.sum()
        val difference = if (amountVal != null) abs(amountVal - totalSplit) else 0.0
        val balanceError = if (difference > 0.02) "Сумма сплита не совпадает с общей суммой" else null

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
                .onError { message, _ ->
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowSnackbar(message))
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