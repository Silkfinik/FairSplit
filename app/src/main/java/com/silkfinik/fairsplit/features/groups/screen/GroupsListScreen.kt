package com.silkfinik.fairsplit.features.groups.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.ui.common.ObserveAsEvents
import com.silkfinik.fairsplit.features.groups.viewmodel.GroupsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsListScreen(
    viewModel: GroupsViewModel = hiltViewModel(),
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToGroupDetails: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveAsEvents(
        flow = viewModel.uiEvent,
        snackbarHostState = snackbarHostState
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Мои группы") })
        },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.groups.isNotEmpty()) {
                FloatingActionButton(onClick = onNavigateToCreateGroup) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Создать группу")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.groups.isEmpty()) {
                EmptyState(
                    modifier = Modifier.align(Alignment.Center),
                    onCreateClick = onNavigateToCreateGroup
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.groups) { group ->
                        GroupItem(
                            group = group,
                            onClick = { onNavigateToGroupDetails(group.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroupItem(group: Group, onClick: () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Group, 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = group.currency.symbol,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    onCreateClick: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Groups,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Групп пока нет",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Создайте свою первую группу, чтобы начать вести совместный бюджет с друзьями!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Создать группу")
        }
    }
}