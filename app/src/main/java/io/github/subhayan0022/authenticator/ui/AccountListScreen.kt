package io.github.subhayan0022.authenticator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.subhayan0022.authenticator.data.OtpType
import kotlin.math.roundToInt
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
    onQueryChange: (String) -> Unit,
    onGroupSelected: (String?) -> Unit,
    onCreateGroup: (String) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onDeleteGroup: (String) -> Unit,
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
            onQueryChange = onQueryChange,
            onGroupSelected = onGroupSelected,
            onCreateGroup = onCreateGroup,
            onRenameGroup = onRenameGroup,
            onDeleteGroup = onDeleteGroup,
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
    onQueryChange: (String) -> Unit,
    onGroupSelected: (String?) -> Unit,
    onCreateGroup: (String) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onDeleteGroup: (String) -> Unit,
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

        is AccountListUiState.Ready -> if (
            state.codes.isEmpty() && state.query.isBlank() && state.selectedGroup == null
        ) {
            Centered(modifier) {
                Text("No accounts yet", style = MaterialTheme.typography.titleLarge)
                Button(onClick = onAddAccountClick) { Text("Add account") }
            }
        } else {
            var pendingDelete by remember { mutableStateOf<AccountCode?>(null) }
            var menuFor by remember { mutableStateOf<Long?>(null) }
            var groupManagerOpen by remember { mutableStateOf(false) }

            var dragFrom by remember { mutableStateOf<Int?>(null) }
            var dragTo by remember { mutableStateOf<Int?>(null) }
            var dragOffset by remember { mutableFloatStateOf(0f) }
            var rowHeight by remember { mutableFloatStateOf(0f) }
            var autoScroll by remember { mutableFloatStateOf(0f) }

            val listState = rememberLazyListState()
            val rowSpacing = with(LocalDensity.current) { 12.dp.toPx() }
            val autoScrollStep = with(LocalDensity.current) { 8.dp.toPx() }

            LaunchedEffect(autoScroll) {
                if (autoScroll == 0f) return@LaunchedEffect

                while (true) {
                    val consumed = listState.scrollBy(autoScroll)
                    if (consumed == 0f) break
                    dragOffset += consumed
                    withFrameNanos { }
                }
            }

            LazyColumn(
                state = listState,
                modifier = modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "search") {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        label = { Text("Search") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item(key = "groups") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state.groups.isNotEmpty()) {
                            FilterChip(
                                selected = state.selectedGroup == null,
                                onClick = { onGroupSelected(null) },
                                label = { Text("All") },
                            )
                        }

                        state.groups.forEach { group ->
                            FilterChip(
                                selected = state.selectedGroup == group,
                                onClick = { onGroupSelected(group) },
                                label = { Text(group) },
                            )
                        }

                        TextButton(onClick = { groupManagerOpen = true }) {
                            Text("Groups")
                        }
                    }
                }

                if (state.codes.isEmpty()) {
                    item(key = "no-matches") {
                        Text(
                            "No accounts match that search.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }

                if (showBackupReminder) {
                    item(key = "backup-reminder") {
                        BackupReminderCard(
                            onBackupNow = onBackupNow,
                            onDismiss = onDismissReminder,
                        )
                    }
                }

                itemsIndexed(state.codes, key = { _, it -> it.account.id }) { index, item ->
                    val liveIndex by rememberUpdatedState(index)
                    val liveLastIndex by rememberUpdatedState(state.codes.lastIndex)

                    val from = dragFrom
                    val to = dragTo

                    val shift = when {
                        from == null || to == null -> 0f
                        index == from -> dragOffset
                        to > from && index in (from + 1)..to -> -rowHeight
                        to < from && index in to until from -> rowHeight
                        else -> 0f
                    }

                    AccountRow(
                        item = item,
                        dragging = index == from,
                        menuOpen = menuFor == item.account.id,
                        onMenuOpen = { menuFor = item.account.id },
                        onMenuDismiss = { menuFor = null },
                        onCopy = { onCopyCode(item.code) },
                        onEdit = {
                            menuFor = null
                            onEditAccount(item.account.id)
                        },
                        onDelete = {
                            menuFor = null
                            pendingDelete = item
                        },
                        onAdvanceHotp = {
                            menuFor = null
                            onAdvanceHotp(item.account.id)
                        },
                        modifier = Modifier
                            .zIndex(if (index == from) 1f else 0f)
                            .graphicsLayer { translationY = shift }
                            .onSizeChanged { rowHeight = it.height.toFloat() + rowSpacing }
                            .pointerInput(item.account.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        dragFrom = liveIndex
                                        dragTo = liveIndex
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount.y
                                        if (rowHeight > 0f) {
                                            val moved = (dragOffset / rowHeight).roundToInt()
                                            dragTo = (liveIndex + moved)
                                                .coerceIn(0, liveLastIndex)
                                        }

                                        val layout = listState.layoutInfo
                                        val onScreen = layout.visibleItemsInfo
                                            .firstOrNull { it.key == item.account.id }

                                        autoScroll = if (onScreen == null) {
                                            0f
                                        } else {
                                            val top = onScreen.offset + dragOffset
                                            val bottom = top + onScreen.size
                                            val edge = onScreen.size * 0.5f

                                            when {
                                                top < layout.viewportStartOffset + edge ->
                                                    -autoScrollStep

                                                bottom > layout.viewportEndOffset - edge ->
                                                    autoScrollStep

                                                else -> 0f
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        val startIndex = dragFrom
                                        val endIndex = dragTo
                                        if (startIndex != null && endIndex != null &&
                                            startIndex != endIndex
                                        ) {
                                            onMove(item.account.id, endIndex - startIndex)
                                        }
                                        dragFrom = null
                                        dragTo = null
                                        dragOffset = 0f
                                        autoScroll = 0f
                                    },
                                    onDragCancel = {
                                        dragFrom = null
                                        dragTo = null
                                        dragOffset = 0f
                                        autoScroll = 0f
                                    },
                                )
                            },
                    )
                }
            }

            if (groupManagerOpen) {
                GroupManagerDialog(
                    groups = state.groups,
                    onDismiss = { groupManagerOpen = false },
                    onCreate = onCreateGroup,
                    onRename = onRenameGroup,
                    onDelete = onDeleteGroup,
                )
            }

            pendingDelete?.let { target ->
                OwnOverlay()

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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "No backup yet. These secrets exist only on this " +
                    "device and would be lost with it."
            },
    ) {
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

@Composable
private fun AccountRow(
    item: AccountCode,
    dragging: Boolean,
    menuOpen: Boolean,
    onMenuOpen: () -> Unit,
    onMenuDismiss: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdvanceHotp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = spoken(item) }
            .clickable(onClickLabel = "Copy code", onClick = onCopy),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
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

            Box {
                IconButton(onClick = onMenuOpen) {
                    Text("\u22EE", style = MaterialTheme.typography.titleLarge)
                }

                DropdownMenu(expanded = menuOpen, onDismissRequest = onMenuDismiss) {
                    OwnOverlay()

                    DropdownMenuItem(text = { Text("Edit") }, onClick = onEdit)

                    if (item.account.type == OtpType.HOTP) {
                        DropdownMenuItem(text = { Text("Next code") }, onClick = onAdvanceHotp)
                    }

                    DropdownMenuItem(text = { Text("Delete") }, onClick = onDelete)
                }
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

private fun spoken(item: AccountCode): String {
    val digits = item.code.filter(Char::isDigit).toCharArray().joinToString(" ")
    val account = item.account.label.ifBlank { "no account name" }

    return when (val remaining = item.secondsRemaining) {
        null -> "${item.account.issuer}, $account, code $digits, counter based"
        else -> "${item.account.issuer}, $account, code $digits, expires in $remaining seconds"
    }
}

private fun String.grouped(): String =
    if (length == 6) "${substring(0, 3)} ${substring(3)}" else this
