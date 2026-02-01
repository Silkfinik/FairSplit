package com.silkfinik.fairsplit.features.payments.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.common.util.onError
import com.silkfinik.fairsplit.core.common.util.onSuccess
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.usecase.group.GetGroupUseCase
import com.silkfinik.fairsplit.core.domain.usecase.member.GetMembersUseCase
import com.silkfinik.fairsplit.core.domain.usecase.payment.CreatePaymentUseCase
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import com.silkfinik.fairsplit.features.payments.ui.CreatePaymentUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getGroupUseCase: GetGroupUseCase,
    private val getMembersUseCase: GetMembersUseCase,
    private val createPaymentUseCase: CreatePaymentUseCase,
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])
    private val prefillReceiverId: String? = savedStateHandle["receiverId"]
    private val prefillAmount: String? = savedStateHandle["amount"]

    private val _uiState = MutableStateFlow(CreatePaymentUiState())
    val uiState: StateFlow<CreatePaymentUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val group = getGroupUseCase(groupId).first() ?: throw Exception("Группа не найдена")
                val members = getMembersUseCase(groupId).first()
                val currentUserId = authRepository.getUserId()

                val displayMembers = members.filter { it.mergedWithUid == null }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currency = group.currency,
                        members = displayMembers,
                        currentUserId = currentUserId,
                        payerId = currentUserId,
                        receiverId = prefillReceiverId,
                        amount = prefillAmount ?: ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onAmountChange(amount: String) {
        val doubleVal = amount.toDoubleOrNull()
        val error = if (amount.isNotBlank() && (doubleVal == null || doubleVal <= 0)) "Некорректная сумма" else null
        _uiState.update { it.copy(amount = amount, amountError = error) }
    }

    fun onReceiverChange(receiverId: String) {
        _uiState.update { it.copy(receiverId = receiverId, receiverError = null) }
    }

    fun onSaveClick() {
        val state = _uiState.value
        val amountVal = state.amount.toDoubleOrNull()
        
        val amountError = if (amountVal == null || amountVal <= 0) "Введите сумму" else null
        val receiverError = if (state.receiverId == null) "Выберите получателя" else if (state.receiverId == state.payerId) "Нельзя перевести самому себе" else null

        if (amountError != null || receiverError != null) {
            _uiState.update {
                it.copy(amountError = amountError, receiverError = receiverError)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val params = CreatePaymentUseCase.Params(
                groupId = groupId,
                payerId = state.payerId!!,
                receiverId = state.receiverId!!,
                amount = amountVal!!
            )

            createPaymentUseCase(params)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                    sendEvent(UiEvent.NavigateBack)
                }
                .onError { message, _ ->
                    _uiState.update { it.copy(isLoading = false, error = message) }
                    sendEvent(UiEvent.ShowSnackbar(message))
                }
        }
    }
}
