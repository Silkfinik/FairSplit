package com.silkfinik.fairsplit.core.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silkfinik.fairsplit.core.common.util.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    protected fun sendEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }
}
