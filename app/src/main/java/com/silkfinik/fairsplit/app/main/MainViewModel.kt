package com.silkfinik.fairsplit.app.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.NetworkMonitor
import com.silkfinik.fairsplit.core.data.repository.AuthRepository
import com.silkfinik.fairsplit.core.data.sync.GroupSynchronizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor,
    private val groupSynchronizer: GroupSynchronizer
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        checkAuthAndNetwork()
    }

    fun retry() {
        checkAuthAndNetwork()
    }

    private fun checkAuthAndNetwork() {
        viewModelScope.launch {
            _uiState.value = MainUiState.Loading

            if (authRepository.hasSession()) {
                groupSynchronizer.startListening()
                _uiState.value = MainUiState.Success
                return@launch
            }

            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline) {
                    val result = authRepository.signInAnonymously()
                    if (result.isSuccess) {
                        groupSynchronizer.startListening()
                        _uiState.value = MainUiState.Success
                    } else {
                        _uiState.value = MainUiState.ErrorAuthFailed("Ошибка входа: ${result.exceptionOrNull()?.message}")
                    }
                } else {
                    _uiState.value = MainUiState.ErrorNoInternet
                }
            }
        }
    }
}