package com.silkfinik.fairsplit.features.account.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silkfinik.fairsplit.core.model.User
import com.silkfinik.fairsplit.core.ui.common.ObserveAsEvents
import com.silkfinik.fairsplit.core.ui.component.FairSplitCard
import com.silkfinik.fairsplit.core.ui.component.FairSplitTextField
import com.silkfinik.fairsplit.core.ui.component.FairSplitTopAppBar
import com.silkfinik.fairsplit.core.ui.component.FairSplitUserAvatar
import com.silkfinik.fairsplit.features.account.viewmodel.AccountViewModel

@Composable
fun AccountScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLinkEmailDialog by remember { mutableStateOf(false) }
    var showChangeEmailDialog by remember { mutableStateOf(false) }

    var nameState by remember(uiState.user) { mutableStateOf(uiState.user?.displayName ?: "") }

    ObserveAsEvents(
        flow = viewModel.uiEvent,
        snackbarHostState = snackbarHostState
    )


    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Выход") },
            text = { Text("Вы уверены, что хотите выйти из аккаунта?") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    viewModel.onSignOut()
                }) { Text("Выйти") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удаление аккаунта") },
            text = { Text("Это действие необратимо. Все ваши данные будут удалены. Продолжить?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.onDeleteAccount()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showLinkEmailDialog) {
        LinkEmailDialog(
            onDismiss = { showLinkEmailDialog = false },
            onConfirm = { email, password ->
                showLinkEmailDialog = false
                viewModel.onLinkEmailAccount(email, password)
            }
        )
    }

    if (showChangeEmailDialog) {
        ChangeEmailDialog(
            currentEmail = uiState.user?.email ?: "",
            onDismiss = { showChangeEmailDialog = false },
            onConfirm = { newEmail ->
                showChangeEmailDialog = false
                viewModel.updateEmail(newEmail)
            }
        )
    }

    Scaffold(
        topBar = {
            FairSplitTopAppBar(
                title = if (uiState.isAnonymous) "Гостевой профиль" else "Профиль",
                onBackClick = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading && uiState.user == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProfileHeader(
                    user = uiState.user,
                    name = nameState,
                    isAnonymous = uiState.isAnonymous,
                    onNameChange = { nameState = it },
                    onNameSubmit = { viewModel.onNameChange(nameState) },
                    onAvatarSelected = viewModel::onAvatarSelected
                )

                if (!uiState.isAnonymous && !uiState.isEmailVerified) {
                    VerificationWarningCard(
                        onResendClick = viewModel::resendVerificationEmail,
                        onCheckStatusClick = viewModel::checkVerificationStatus,
                        onChangeEmailClick = { showChangeEmailDialog = true }
                    )
                }

                if (uiState.isAnonymous) {
                    LinkAccountSection(
                        onLinkGoogle = { viewModel.startGoogleAccountLink(context) },
                        onLinkEmail = { showLinkEmailDialog = true }
                    )
                }

                SettingsSection(
                    isNotificationsEnabled = uiState.isNotificationsEnabled,
                    onToggleNotifications = viewModel::onToggleNotifications,
                    onSignOut = { showSignOutDialog = true },
                    onDeleteAccount = { showDeleteDialog = true }
                )
            }
        }
    }
}

@Composable
fun ProfileHeader(
    user: User?,
    name: String,
    isAnonymous: Boolean,
    onNameChange: (String) -> Unit,
    onNameSubmit: () -> Unit,
    onAvatarSelected: (Uri) -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onAvatarSelected(uri)
            }
        }
    )

    FairSplitCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.clickable {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                FairSplitUserAvatar(
                    photoUrl = user?.photoUrl,
                    name = user?.displayName ?: "?",
                    size = 100.dp,
                    fontSize = 40.sp
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(32.dp)
                        .offset(x = 4.dp, y = 4.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Изменить фото",
                        modifier = Modifier.padding(6.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (user?.email != null) {
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
            } else if (isAnonymous) {
                Text(
                    text = "Гостевой режим",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            FairSplitTextField(
                value = name,
                onValueChange = onNameChange,
                label = "Имя",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Введите ваше имя") }
            )

            if (name != user?.displayName && name.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNameSubmit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Сохранить")
                }
            }
        }
    }
}

@Composable
fun LinkAccountSection(
    onLinkGoogle: () -> Unit,
    onLinkEmail: () -> Unit
) {
    FairSplitCard {
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Сохранить прогресс",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Text(
                text = "Привяжите аккаунт, чтобы не потерять данные при выходе или смене устройства.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onLinkGoogle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Привязать Google")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onLinkEmail,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Привязать Email")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun SettingsSection(
    isNotificationsEnabled: Boolean,
    onToggleNotifications: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    FairSplitCard {
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(text = "Уведомления")
                }
                Switch(
                    checked = isNotificationsEnabled,
                    onCheckedChange = onToggleNotifications
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            ListItem(
                headlineContent = { Text("Выйти") },
                leadingContent = {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable(onClick = onSignOut),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            ListItem(
                headlineContent = {
                    Text("Удалить аккаунт", color = MaterialTheme.colorScheme.error)
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.clickable(onClick = onDeleteAccount),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}

@Composable
fun LinkEmailDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Привязка Email") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FairSplitTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    modifier = Modifier.fillMaxWidth()
                )
                FairSplitTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Пароль",
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.isNotBlank() && password.length >= 6) {
                        onConfirm(email, password)
                    }
                },
                enabled = email.isNotBlank() && password.length >= 6
            ) { Text("Привязать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun VerificationWarningCard(
    onResendClick: () -> Unit,
    onCheckStatusClick: () -> Unit,
    onChangeEmailClick: () -> Unit
) {
    FairSplitCard(
        backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.MarkEmailUnread,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Почта не подтверждена",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Вы не сможете войти в аккаунт повторно, пока не подтвердите почту по ссылке в письме.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onChangeEmailClick) {
                    Text("Исправить почту")
                }
                TextButton(onClick = onResendClick) {
                    Text("Выслать письмо")
                }
            }
            Button(
                onClick = onCheckStatusClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Я подтвердил")
            }
        }
    }
}

@Composable
fun ChangeEmailDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newEmail by remember { mutableStateOf(currentEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Исправить Email") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Введите корректный адрес. Мы отправим на него новое письмо с подтверждением.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FairSplitTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = "Новый Email",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newEmail.isNotBlank() && newEmail != currentEmail) {
                        onConfirm(newEmail)
                    }
                },
                enabled = newEmail.isNotBlank() && newEmail != currentEmail
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}