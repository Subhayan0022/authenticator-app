package io.github.subhayan0022.authenticator.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.subhayan0022.authenticator.data.OtpType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    state: AccountListUiState,
    onUnlockClick: () -> Unit,
    onAddAccountClick: () -> Unit,
    onDelete: (Long) -> Unit,
    onEditAccount: (Long) -> Unit,
    onMove: (Long, Int) -> Unit,
    onAdvanceHotp: (Long) -> Unit,
    onCopyCode: (String) -> Unit,
    onSettingsClick: () -> Unit,
    showBackupReminder: Boolean,
    onBackupNow: () -> Unit,
    onDismissReminder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val copyAndNotify: (String) -> Unit = { code ->
        onCopyCode(code)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar("Code copied - clears in 30s")
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Authenticator") },
                actions = { TextButton(onClick = onSettingsClick) { Text("Settings") } },
            )
        },
        floatingActionButton = {
            if (state is AccountListUiState.Ready && state.codes.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddAccountClick,
                    text = { Text("Add account") },
                    icon = {},
                )
            }
        },
    ) { innerPadding ->
        AccountListContent(
            state = state,
            onUnlockClick = onUnlockClick,
            onAddAccountClick = onAddAccountClick,
            onDelete = onDelete,
            onEditAccount = onEditAccount,
            onMove = onMove,
            onAdvanceHotp = onAdvanceHotp,
            onCopyCode = copyAndNotify,
            showBackupReminder = showBackupReminder,
            onBackupNow = onBackupNow,
            onDismissReminder = onDismissReminder,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun AccountListContent(
    state: AccountListUiState,
    onUnlockClick: () -> Unit,
    onAddAccountClick: () -> Unit,
    onDelete: (Long) -> Unit,
    onEditAccount: (Long) -> Unit,
    onMove: (Long, Int) -> Unit,
    onAdvanceHotp: (Long) -> Unit,
    onCopyCode: (String) -> Unit,
    showBackupReminder: Boolean,
    onBackupNow: () -> Unit,
    onDismissReminder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        AccountListUiState.Loading -> Centered(modifier) {
            CircularProgressIndicator()
        }

        AccountListUiState.Locked -> Centered(modifier) {
            Text("Locked", style = MaterialTheme.typography.titleLarge)
            Text(
                "Authenticate to view your codes.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onUnlockClick) { Text("Unlock") }
        }

        is AccountListUiState.Ready -> if (state.codes.isEmpty()) {
            Centered(modifier) {
                Text("No accounts yet", style = MaterialTheme.typography.titleLarge)
                Button(onClick = onAddAccountClick) { Text("Add account") }
            }
        } else {
            var selected by remember { mutableStateOf<AccountCode?>(null) }
            var pendingDelete by remember { mutableStateOf<AccountCode?>(null) }

            LazyColumn(
                modifier = modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (showBackupReminder) {
                    item(key = "backup-reminder") {
                        BackupReminderCard(
                            onBackupNow = onBackupNow,
                            onDismiss = onDismissReminder,
                        )
                    }
                }

                items(state.codes, key = { it.account.id }) { item ->
                    AccountRow(
                        item = item,
                        onCopy = { onCopyCode(item.code) },
                        onLongPress = { selected = item },
                    )
                }
            }

            selected?.let { target ->
                val index = state.codes.indexOfFirst { it.account.id == target.account.id }

                AlertDialog(
                    onDismissRequest = { selected = null },
                    title = { Text(target.account.issuer) },
                    text = {
                        Column {
                            TextButton(
                                onClick = {
                                    selected = null
                                    onEditAccount(target.account.id)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Edit")
                            }

                            if (target.account.type == OtpType.HOTP) {
                                TextButton(
                                    onClick = {
                                        selected = null
                                        onAdvanceHotp(target.account.id)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Next code")
                                }
                            }

                            TextButton(
                                onClick = {
                                    selected = null
                                    onMove(target.account.id, -1)
                                },
                                enabled = index > 0,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Move up")
                            }

                            TextButton(
                                onClick = {
                                    selected = null
                                    onMove(target.account.id, 1)
                                },
                                enabled = index in 0 until state.codes.lastIndex,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Move down")
                            }

                            TextButton(
                                onClick = {
                                    pendingDelete = target
                                    selected = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Delete")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { selected = null }) { Text("Cancel") }
                    },
                )
            }

            pendingDelete?.let { target ->
                AlertDialog(
                    onDismissRequest = { pendingDelete = null },
                    title = { Text("Delete ${target.account.issuer}?") },
                    text = {
                        Text(
                            "This permanently removes the secret for " +
                                "${target.account.label.ifBlank { "this account" }}. " +
                                "You will not be able to generate codes for it again " +
                                "unless you re-add it from the original QR code.",
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onDelete(target.account.id)
                                pendingDelete = null
                            },
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
                    },
                )
            }
        }
    }
}

@Composable
private fun BackupReminderCard(onBackupNow: () -> Unit, onDismiss: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("No backup yet", style = MaterialTheme.typography.titleSmall)
            Text(
                "These secrets exist only on this device. Losing it, or changing " +
                    "the screen lock, loses every account permanently.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onBackupNow) { Text("Back up now") }
                TextButton(onClick = onDismiss) { Text("Not now") }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountRow(
    item: AccountCode,
    onCopy: () -> Unit,
    onLongPress: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onCopy, onLongClick = onLongPress),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(item.account.issuer, style = MaterialTheme.typography.titleMedium)
            Text(item.account.label, style = MaterialTheme.typography.bodySmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(item.code.grouped(), style = MaterialTheme.typography.headlineMedium)
                item.secondsRemaining?.let { Text("${it}s") }
            }

            item.secondsRemaining?.let { remaining ->
                LinearProgressIndicator(
                    progress = { remaining.toFloat() / item.account.periodSeconds },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Centered(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

private fun String.grouped(): String =
    if (length == 6) "${substring(0, 3)} ${substring(3)}" else this
