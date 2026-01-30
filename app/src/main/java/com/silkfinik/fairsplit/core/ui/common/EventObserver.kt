package com.silkfinik.fairsplit.core.ui.common

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.silkfinik.fairsplit.core.common.util.UiEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ObserveAsEvents(
    flow: Flow<UiEvent>,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit = {},
    onNavigateToGroupDetails: (String) -> Unit = {}
) {
    LaunchedEffect(key1 = true) {
        flow.collectLatest { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                UiEvent.NavigateBack -> {
                    onNavigateBack()
                }
                is UiEvent.NavigateToGroupDetails -> {
                    onNavigateToGroupDetails(event.groupId)
                }
            }
        }
    }
}
