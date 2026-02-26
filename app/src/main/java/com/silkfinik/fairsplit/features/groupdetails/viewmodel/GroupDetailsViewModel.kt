package com.silkfinik.fairsplit.features.groupdetails.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.R
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.common.util.UiText
import com.silkfinik.fairsplit.core.common.util.asUiText
import com.silkfinik.fairsplit.core.common.util.onError
import com.silkfinik.fairsplit.core.common.util.onSuccess
import com.silkfinik.fairsplit.core.domain.usecase.expense.DeleteExpenseUseCase
import com.silkfinik.fairsplit.core.domain.usecase.expense.SyncGroupExpensesUseCase
import com.silkfinik.fairsplit.core.domain.usecase.group.GenerateInviteCodeUseCase
import com.silkfinik.fairsplit.core.domain.usecase.group.GetGroupDetailsScreenDataUseCase
import com.silkfinik.fairsplit.core.domain.usecase.group.UpdateGroupAvatarUseCase
import com.silkfinik.fairsplit.core.domain.usecase.group.UpdateGroupNameUseCase
import com.silkfinik.fairsplit.core.domain.usecase.member.AddGhostMemberUseCase
import com.silkfinik.fairsplit.core.domain.usecase.payment.SyncGroupPaymentsUseCase
import com.silkfinik.fairsplit.core.domain.usecase.payment.UpdatePaymentStatusUseCase
import com.silkfinik.fairsplit.core.model.enums.PaymentStatus
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import com.silkfinik.fairsplit.features.groupdetails.ui.GroupDetailsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getGroupDetailsScreenDataUseCase: GetGroupDetailsScreenDataUseCase,
    private val addGhostMemberUseCase: AddGhostMemberUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val syncGroupExpensesUseCase: SyncGroupExpensesUseCase,
    private val syncGroupPaymentsUseCase: SyncGroupPaymentsUseCase,
    private val generateInviteCodeUseCase: GenerateInviteCodeUseCase,
    private val updatePaymentStatusUseCase: UpdatePaymentStatusUseCase,
    private val updateGroupNameUseCase: UpdateGroupNameUseCase,
    private val updateGroupAvatarUseCase: UpdateGroupAvatarUseCase
) : BaseViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _isGeneratingCode = MutableStateFlow(false)
    val isGeneratingCode = _isGeneratingCode.asStateFlow()

    private val _isUpdatingGroup = MutableStateFlow(false)
    val isUpdatingGroup = _isUpdatingGroup.asStateFlow()

    val uiState: StateFlow<GroupDetailsUiState> = getGroupDetailsScreenDataUseCase(groupId)
        .map { data ->
            if (data.group == null) {
                GroupDetailsUiState.Error(UiText.StringResource(R.string.error_group_not_found))
            } else {
                GroupDetailsUiState.Success(
                    group = data.group,
                    members = data.members,
                    expenses = data.expenses,
                    payments = data.payments,
                    balances = data.balances,
                    currentUserId = data.currentUserId
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GroupDetailsUiState.Loading
        )

    init {
        syncGroupExpensesUseCase.start(groupId)
        syncGroupPaymentsUseCase.start(groupId)
    }

    override fun onCleared() {
        super.onCleared()
        syncGroupExpensesUseCase.stop(groupId)
        syncGroupPaymentsUseCase.stop(groupId)
    }

    fun addGhostMember(name: String) {
        viewModelScope.launch {
            addGhostMemberUseCase(groupId, name)
                .onError { error ->
                    sendEvent(UiEvent.ShowError(error.asUiText()))
                }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            deleteExpenseUseCase(expenseId)
                .onError { error ->
                    sendEvent(UiEvent.ShowError(error.asUiText()))
                }
        }
    }

    fun updatePaymentStatus(paymentId: String, newStatus: PaymentStatus) {
        viewModelScope.launch {
            updatePaymentStatusUseCase(paymentId, newStatus)
                .onError { error ->
                    sendEvent(UiEvent.ShowError(error.asUiText()))
                }
                .onSuccess {
                    sendEvent(UiEvent.ShowSnackbar(UiText.StringResource(R.string.success_status_updated)))
                }
        }
    }

    fun generateInviteCode() {
        viewModelScope.launch {
            _isGeneratingCode.value = true
            generateInviteCodeUseCase(groupId)
                .onError { error ->
                    sendEvent(UiEvent.ShowError(error.asUiText()))
                }
            _isGeneratingCode.value = false
        }
    }

    fun updateGroupDetails(newName: String, newAvatarUri: String?) {
        if (newName.isBlank()) {
            sendEvent(UiEvent.ShowError(UiText.StringResource(R.string.error_empty_group_name)))
            return
        }
        viewModelScope.launch {
            _isUpdatingGroup.value = true

            val currentGroup = (uiState.value as? GroupDetailsUiState.Success)?.group
            var isChanged = false

            if (currentGroup?.name != newName) {
                updateGroupNameUseCase(groupId, newName)
                    .onError { error ->
                        sendEvent(UiEvent.ShowError(error.asUiText()))
                        _isUpdatingGroup.value = false
                        return@launch
                    }
                isChanged = true
            }

            if (newAvatarUri != null && newAvatarUri != currentGroup?.avatarUrl && !newAvatarUri.startsWith("http")) {
                updateGroupAvatarUseCase(groupId, newAvatarUri)
                    .onError { error ->
                        sendEvent(UiEvent.ShowError(error.asUiText()))
                        _isUpdatingGroup.value = false
                        return@launch
                    }
                isChanged = true
            }

            if (isChanged) {
                sendEvent(UiEvent.ShowSnackbar(UiText.StringResource(R.string.success_group_updated)))
            }
            _isUpdatingGroup.value = false
        }
    }
}