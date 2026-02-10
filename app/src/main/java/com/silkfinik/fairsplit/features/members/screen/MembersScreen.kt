package com.silkfinik.fairsplit.features.members.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.core.ui.component.FairSplitUserAvatar
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.ui.common.ObserveAsEvents
import com.silkfinik.fairsplit.core.ui.component.FairSplitCard
import com.silkfinik.fairsplit.core.ui.component.FairSplitTopAppBar
import com.silkfinik.fairsplit.core.ui.component.FairSplitTextField
import com.silkfinik.fairsplit.features.members.ui.MembersUiState
import com.silkfinik.fairsplit.features.members.viewmodel.MembersViewModel

@Composable
fun MembersScreen(
    viewModel: MembersViewModel = hiltViewModel(),
    onBack: () -> Unit
) {

    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsState()
    val isGeneratingCode by viewModel.isGeneratingCode.collectAsState()
    
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var memberToEdit by remember { mutableStateOf<Member?>(null) }
    var memberToClaim by remember { mutableStateOf<Member?>(null) }
    var showClaimInfo by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    ObserveAsEvents(
        flow = viewModel.uiEvent,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onBack
    )

    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onConfirm = { name ->
                viewModel.addGhostMember(name)
                showAddMemberDialog = false
            }
        )
    }

    if (memberToEdit != null) {
        EditMemberNameDialog(
            member = memberToEdit!!,
            onDismiss = { memberToEdit = null },
            onConfirm = { id, name ->
                viewModel.updateMemberName(id, name)
                memberToEdit = null
            }
        )
    }

    if (showClaimInfo) {
        AlertDialog(
            onDismissRequest = { showClaimInfo = false },
            title = { Text("Что такое \"Это я\"?") },
            text = { 
                Text("Если ваш друг добавил вас в группу вручную до того, как вы зарегистрировались, вы можете объединить этот виртуальный профиль со своим аккаунтом. Вся история трат этого профиля перейдет к вам.") 
            },
            confirmButton = {
                TextButton(onClick = { showClaimInfo = false }) {
                    Text("Понятно")
                }
            }
        )
    }

    if (memberToClaim != null) {
        AlertDialog(
            onDismissRequest = { memberToClaim = null },
            title = { Text("Объединение профиля") },
            text = { Text("Вы действительно хотите объединить свой аккаунт с участником \"${memberToClaim?.name}\"? История трат будет сохранена за вами.") },
            confirmButton = {
                Button(
                    onClick = {
                        memberToClaim?.let { viewModel.claimGhost(it.id) }
                        memberToClaim = null
                    }
                ) {
                    Text("Объединить")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToClaim = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            FairSplitTopAppBar(title = "Участники", onBackClick = onBack)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddMemberDialog = true }) {
                Icon(Icons.Default.Add, "Добавить")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                MembersUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is MembersUiState.Error -> {
                    Text(
                        text = state.message.asString(context),
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is MembersUiState.Success -> {
                    MembersList(
                        state = state,
                        isGeneratingCode = isGeneratingCode,
                        onGenerateCode = viewModel::generateInviteCode,
                        onCopyCode = { code ->
                            clipboardManager.setText(AnnotatedString(code))
                        },
                        onClaimClick = { memberToClaim = it },
                        onClaimInfoClick = { showClaimInfo = true },
                        onEditClick = { memberToEdit = it }
                    )
                }
            }
        }
    }
}

@Composable
fun MembersList(
    state: MembersUiState.Success,
    isGeneratingCode: Boolean,
    onGenerateCode: () -> Unit,
    onCopyCode: (String) -> Unit,
    onClaimClick: (Member) -> Unit,
    onClaimInfoClick: () -> Unit,
    onEditClick: (Member) -> Unit
) {
    val members = state.members
    val activeMembers = members.filter { it.mergedWithUid == null && !it.isGhost || it.id == state.currentUserId }
    val ghostMembers = members.filter { it.isGhost && it.mergedWithUid == null && it.id != state.currentUserId }

    val mergedGhostsMap = members
        .filter { it.mergedWithUid != null }
        .groupBy { it.mergedWithUid!! }
        .mapValues { entry -> entry.value.map { it.name } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        item {
            InviteSection(
                inviteCode = state.inviteCode,
                isGenerating = isGeneratingCode,
                onGenerateCode = onGenerateCode,
                onCopyCode = onCopyCode
            )
        }

        if (members.size == 1 && members.first().id == state.currentUserId) {
            item {
                FirstTimeTip()
            }
        }

        if (activeMembers.isNotEmpty()) {
            item {
                SectionHeader(title = "В приложении", count = activeMembers.size)
            }
            items(activeMembers, key = { it.id }) { member ->
                MemberItem(
                    member = member,
                    currentUserId = state.currentUserId,
                    isLinked = state.linkedGhostIds.contains(member.id),
                    mergedGhosts = mergedGhostsMap[member.id] ?: emptyList(),
                    canClaim = false,
                    onClaimClick = {},
                    onClaimInfoClick = {},
                    onEditClick = {}
                )
            }
        }

        if (ghostMembers.isNotEmpty()) {
            item {
                SectionHeader(title = "Виртуальные участники", count = ghostMembers.size)
            }
            items(ghostMembers, key = { it.id }) { member ->
                MemberItem(
                    member = member,
                    currentUserId = state.currentUserId,
                    isLinked = state.linkedGhostIds.contains(member.id),
                    mergedGhosts = emptyList(),
                    canClaim = !state.hasClaimedGhost,
                    onClaimClick = { onClaimClick(member) },
                    onClaimInfoClick = onClaimInfoClick,
                    onEditClick = { onEditClick(member) }
                )
            }
        }
    }
}

@Composable
fun FirstTimeTip() {
    FairSplitCard(
        modifier = Modifier.padding(16.dp),
        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Пока вы в группе один. Добавьте друзей вручную или поделитесь кодом приглашения!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
fun InviteSection(
    inviteCode: String?,
    isGenerating: Boolean,
    onGenerateCode: () -> Unit,
    onCopyCode: (String) -> Unit
) {
    FairSplitCard(
        modifier = Modifier.padding(16.dp),
        backgroundColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Пригласить друзей",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Используйте код, чтобы друзья могли присоединиться сами.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (inviteCode == null) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Button(
                        onClick = onGenerateCode,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Создать код")
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = inviteCode,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(onClick = { onCopyCode(inviteCode) }) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Копировать",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MemberItem(
    member: Member,
    currentUserId: String?,
    isLinked: Boolean,
    mergedGhosts: List<String>,
    canClaim: Boolean,
    onClaimClick: () -> Unit,
    onClaimInfoClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val isCurrentUser = member.id == currentUserId
    
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        FairSplitCard {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FairSplitUserAvatar(
                    photoUrl = member.photoUrl,
                    name = member.name,
                    size = 48.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = "ВЫ",
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    
                    if (mergedGhosts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            mergedGhosts.forEach { ghostName ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        text = ghostName,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (member.isGhost && member.mergedWithUid == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (canClaim && !isLinked) {
                            IconButton(onClick = onClaimInfoClick) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = "Инфо",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            TextButton(
                                onClick = onClaimClick,
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Это я", fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        IconButton(onClick = onEditClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Редактировать",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun membersContainsUid(member: Member, uid: String?): Boolean {
    return uid != null && member.mergedWithUid == uid
}

@Composable
fun EditMemberNameDialog(
    member: Member,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(member.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изменить имя") },
        text = {
            FairSplitTextField(
                value = name,
                onValueChange = { name = it },
                label = "Имя участника",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onConfirm(member.id, name) })
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(member.id, name) },
                enabled = name.isNotBlank() && name != member.name
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый участник") },
        text = {
            Column {
                Text(
                    text = "Добавьте друга вручную. Вы сможете делить с ним траты, а позже он сможет присоединиться к группе.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                FairSplitTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Имя участника",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (name.isNotBlank()) {
                                onConfirm(name)
                            }
                        }
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Отмена")
            }
        }
    )
}