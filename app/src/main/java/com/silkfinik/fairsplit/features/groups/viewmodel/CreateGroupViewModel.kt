package com.silkfinik.fairsplit.features.groups.viewmodel

import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.domain.usecase.group.CreateGroupUseCase
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import com.silkfinik.fairsplit.features.groups.ui.CreateGroupUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val createGroupUseCase: CreateGroupUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    fun onNameChange(newValue: String) {
        _uiState.update { it.copy(name = newValue) }
    }

    fun onCurrencyChange(newCurrency: Currency) {
        _uiState.update { it.copy(selectedCurrency = newCurrency) }
    }

    fun createGroup(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        if (currentState.name.isBlank()) {
            sendEvent(UiEvent.ShowSnackbar("Введите название группы"))
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val result = createGroupUseCase(currentState.name, currentState.selectedCurrency)
            
            when (result) {
                is Result.Success -> {
                    onSuccess()
                    _uiState.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowSnackbar(result.message))
                }
            }
        }
    }
}