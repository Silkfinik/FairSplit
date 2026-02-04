package com.silkfinik.fairsplit.features.payments.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.core.ui.common.ObserveAsEvents
import com.silkfinik.fairsplit.core.ui.component.FairSplitTopAppBar
import com.silkfinik.fairsplit.core.ui.component.FairSplitUserPill
import com.silkfinik.fairsplit.features.expenses.screen.BigAmountInput
import com.silkfinik.fairsplit.features.payments.viewmodel.CreatePaymentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePaymentScreen(
    viewModel: CreatePaymentViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveAsEvents(
        flow = viewModel.uiEvent,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onBack
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            FairSplitTopAppBar(
                title = "Вернуть долг",
                onBackClick = onBack
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                BigAmountInput(
                                    amount = uiState.amount,
                                    onAmountChange = viewModel::onAmountChange,
                                    currency = uiState.currency,
                                    readOnly = false
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Text(
                                    text = "Кто переводит",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    items(uiState.members) { member ->
                                        val isSelected = member.id == uiState.payerId
                                        val displayName = if (member.id == uiState.currentUserId) "${member.name} (Вы)" else member.name
                                        val displayMember = member.copy(name = displayName)

                                        FairSplitUserPill(
                                            member = displayMember,
                                            isSelected = isSelected,
                                            onClick = { }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Кому переводит",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    items(uiState.members) { member ->
                                        if (member.id != uiState.payerId) {
                                            val isSelected = member.id == uiState.receiverId
                                            val displayName = if (member.id == uiState.currentUserId) "${member.name} (Вы)" else member.name
                                            val displayMember = member.copy(name = displayName)

                                            FairSplitUserPill(
                                                member = displayMember,
                                                isSelected = isSelected,
                                                onClick = { viewModel.onReceiverChange(member.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            Button(
                                onClick = viewModel::onSaveClick,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                enabled = uiState.payerId != null && uiState.receiverId != null && uiState.amount.isNotEmpty()
                            ) {
                                Text("Перевести")
                            }
                        }
                    }
                }
            }
        }
    }
}