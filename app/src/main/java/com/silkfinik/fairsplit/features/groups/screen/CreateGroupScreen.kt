package com.silkfinik.fairsplit.features.groups.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.ui.common.ObserveAsEvents
import com.silkfinik.fairsplit.core.ui.component.FairSplitButton
import com.silkfinik.fairsplit.core.ui.component.FairSplitTextField
import com.silkfinik.fairsplit.core.ui.component.FairSplitTopAppBar
import com.silkfinik.fairsplit.features.groups.viewmodel.CreateGroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    viewModel: CreateGroupViewModel = hiltViewModel(),
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
                title = "Новая группа",
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
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))

                            GroupAvatarPreview(name = uiState.name)

                            Spacer(modifier = Modifier.height(32.dp))

                            FairSplitTextField(
                                value = uiState.name,
                                onValueChange = viewModel::onNameChange,
                                label = "Название группы",
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                placeholder = { Text("Например: Поездка на Алтай") }
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            Text(
                                text = "Валюта группы",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            CurrencySelector(
                                selectedCurrency = uiState.selectedCurrency,
                                onCurrencySelected = viewModel::onCurrencyChange
                            )

                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    FairSplitButton(
                        text = "Создать группу",
                        onClick = {
                            viewModel.createGroup(onSuccess = onBack)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.name.isNotBlank() && !uiState.isLoading,
                        isLoading = uiState.isLoading
                    )
                }
            }
        }
    }
}

@Composable
fun GroupAvatarPreview(name: String) {
    val displayChar = if (name.isNotBlank()) name.first().uppercase() else "?"

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(100.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            )
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape
                )
        )

        Text(
            text = displayChar,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CurrencySelector(
    selectedCurrency: Currency,
    onCurrencySelected: (Currency) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(Currency.entries) { currency ->
            val isSelected = currency == selectedCurrency

            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                label = "CardBg"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "CardBorder"
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = backgroundColor,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCurrencySelected(currency) }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 20.dp)
                ) {
                    Text(
                        text = currency.symbol,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currency.code,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}