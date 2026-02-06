package com.silkfinik.fairsplit.features.payments.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.R
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.common.util.UiText
import com.silkfinik.fairsplit.core.common.util.asUiText
import com.silkfinik.fairsplit.core.common.util.onError
import com.silkfinik.fairsplit.core.common.util.onSuccess
import com.silkfinik.fairsplit.core.domain.usecase.payment.CreatePaymentUseCase
import com.silkfinik.fairsplit.core.domain.usecase.payment.PrepareCreatePaymentUseCase
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import com.silkfinik.fairsplit.features.payments.ui.CreatePaymentUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val prepareCreatePaymentUseCase: PrepareCreatePaymentUseCase,
    private val createPaymentUseCase: CreatePaymentUseCase
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

            prepareCreatePaymentUseCase(groupId)
                .onSuccess { data ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currency = data.currency,
                            members = data.members,
                            currentUserId = data.currentUserId,
                            payerId = data.currentUserId,
                            receiverId = prefillReceiverId,
                            amount = prefillAmount ?: ""
                        )
                    }
                }
                .onError { error ->
                    val uiText = error.asUiText()
                    _uiState.update { it.copy(isLoading = false, error = uiText) }
                    sendEvent(UiEvent.ShowSnackbar(uiText))
                }
        }
    }

    fun onAmountChange(amount: String) {
        val doubleVal = amount.toDoubleOrNull()
        val error = if (amount.isNotBlank() && (doubleVal == null || doubleVal <= 0)) {
            UiText.StringResource(R.string.error_invalid_amount)
        } else null

        _uiState.update { it.copy(amount = amount, amountError = error) }
    }

    fun onReceiverChange(receiverId: String) {
        _uiState.update { it.copy(receiverId = receiverId, receiverError = null) }
    }

    fun onSaveClick() {
        val state = _uiState.value
        val amountVal = state.amount.toDoubleOrNull()

        val amountError = if (amountVal == null || amountVal <= 0) {
            UiText.StringResource(R.string.enter_amount)
        } else null

        val receiverError = when (state.receiverId) {
            null -> {
                UiText.StringResource(R.string.select_receiver)
            }
            state.payerId -> {
                UiText.StringResource(R.string.error_payment_self_transfer)
            }
            else -> null
        }

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
                .onError { error ->
                    val uiText = error.asUiText()
                    _uiState.update { it.copy(isLoading = false, error = uiText) }
                    sendEvent(UiEvent.ShowSnackbar(uiText))
                }
        }
    }
}