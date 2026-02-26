package com.silkfinik.fairsplit.features.groups.viewmodel

import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.common.util.asUiText
import com.silkfinik.fairsplit.core.common.util.onError
import com.silkfinik.fairsplit.core.common.util.onSuccess
import com.silkfinik.fairsplit.core.domain.usecase.group.CreateGroupUseCase
import com.silkfinik.fairsplit.core.ui.base.BaseViewModel
import com.silkfinik.fairsplit.features.groups.ui.CreateGroupUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val createGroupUseCase: CreateGroupUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onCurrencyChange(currency: com.silkfinik.fairsplit.core.model.Currency) {
        _uiState.update { it.copy(selectedCurrency = currency) }
    }

    fun onImageSelected(uri: String?) {
        _uiState.update { it.copy(imageUri = uri) }
    }

    fun createGroup(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        if (currentState.name.isBlank()) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            createGroupUseCase(currentState.name, currentState.selectedCurrency, currentState.imageUri)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                .onError { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowError(error.asUiText()))
                }
        }
    }
}