package com.silkfinik.fairsplit.features.groups.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.model.Currency
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
    private val repository: GroupRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

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
        if (currentState.name.isBlank()) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val userId = authRepository.getUserId() ?: return@launch
            repository.createGroup(currentState.name, currentState.selectedCurrency, userId)

            onSuccess()
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}