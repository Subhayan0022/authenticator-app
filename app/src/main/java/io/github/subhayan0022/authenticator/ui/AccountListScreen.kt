package io.github.subhayan0022.authenticator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.subhayan0022.authenticator.data.OtpType
import io.github.subhayan0022.authenticator.ui.theme.CodeStyle
import io.github.subhayan0022.authenticator.ui.theme.IssuerLabelStyle
import io.github.subhayan0022.authenticator.ui.theme.TimerStyle
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val ScreenPadding = 22.dp
private val ListCorner = 16.dp

@Composable
fun AccountListScreen(
    state: AccountListUiState,
    onUnlockClick: () -> Unit,
    onScanAccount: () -> Unit,
    onEnterManually: () -> Unit,
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
    val colors = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val copyAndNotify: (String) -> Unit = { code ->
        onCopyCode(code)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar("Code copied - clears in 30s")
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.background,
        contentColor = colors.onBackground,
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader(onSettingsClick = onSettingsClick)

            Box(Modifier.weight(1f)) {
                when (state) {
                    AccountListUiState.Loading -> Centered { CircularProgressIndicator() }

                    AccountListUiState.Locked -> Centered {
                        Text("Locked", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Authenticate to view your codes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        PillButton(text = "Unlock", onClick = onUnlockClick)
                    }

                    is AccountListUiState.Ready -> ReadyContent(
                        state = state,
                        onScanAccount = onScanAccount,
                        onEnterManually = onEnterManually,
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
                    )
                }

                SnackbarHost(
                    snackbarHostState,
                    Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
                )
            }
        }
    }
}

@Composable
private fun ScreenHeader(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenPadding, end = 8.dp, top = 16.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Authenticator", style = MaterialTheme.typography.titleLarge)

        TextButton(onClick = onSettingsClick) {
            Text(
                "Settings",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadyContent(
    state: AccountListUiState.Ready,
    onScanAccount: () -> Unit,
    onEnterManually: () -> Unit,
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
) {
    val colors = MaterialTheme.colorScheme

    var pendingDelete by remember { mutableStateOf<AccountCode?>(null) }
    var sheetFor by remember { mutableStateOf<AccountCode?>(null) }
    var groupManagerOpen by remember { mutableStateOf(false) }
    var addSheetOpen by remember { mutableStateOf(false) }

    var dragFrom by remember { mutableStateOf<Int?>(null) }
    var dragTo by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableFloatStateOf(0f) }
    var autoScroll by remember { mutableFloatStateOf(0f) }

    val listState = rememberLazyListState()
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

    val nothingAtAll = state.codes.isEmpty() &&
        state.query.isBlank() &&
        state.selectedGroup == null &&
        state.groups.isEmpty()

    if (nothingAtAll) {
        Centered {
            Text("No accounts yet", style = MaterialTheme.typography.titleLarge)
            Text(
                "Add one by scanning a QR code or entering a key.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            PillButton(text = "Add account", onClick = { addSheetOpen = true })
        }

        if (addSheetOpen) {
            AddAccountSheet(
                onScan = {
                    addSheetOpen = false
                    onScanAccount()
                },
                onManual = {
                    addSheetOpen = false
                    onEnterManually()
                },
                onDismiss = { addSheetOpen = false },
            )
        }

        return
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 110.dp),
        ) {
            item(key = "search") {
                SearchField(
                    query = state.query,
                    onQueryChange = onQueryChange,
                    modifier = Modifier.padding(
                        start = ScreenPadding,
                        end = ScreenPadding,
                        top = 12.dp,
                        bottom = 20.dp,
                    ),
                )
            }

            item(key = "groups") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = ScreenPadding, end = ScreenPadding, bottom = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Pill(
                        text = "All",
                        selected = state.selectedGroup == null,
                        onClick = { onGroupSelected(null) },
                    )

                    state.groups.forEach { group ->
                        Pill(
                            text = group,
                            selected = state.selectedGroup == group,
                            onClick = { onGroupSelected(group) },
                        )
                    }

                    Pill(
                        text = "Groups",
                        selected = false,
                        onClick = { groupManagerOpen = true },
                    )
                }
            }

            if (showBackupReminder) {
                item(key = "backup-reminder") {
                    BackupReminderCard(
                        onBackupNow = onBackupNow,
                        onDismiss = onDismissReminder,
                        modifier = Modifier.padding(
                            start = ScreenPadding,
                            end = ScreenPadding,
                            bottom = 22.dp,
                        ),
                    )
                }
            }

            if (state.codes.isEmpty()) {
                item(key = "no-matches") {
                    Text(
                        "No accounts match that search.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(ScreenPadding),
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

                val shape = when {
                    state.codes.size == 1 -> RoundedCornerShape(ListCorner)
                    index == 0 -> RoundedCornerShape(topStart = ListCorner, topEnd = ListCorner)
                    index == state.codes.lastIndex ->
                        RoundedCornerShape(bottomStart = ListCorner, bottomEnd = ListCorner)
                    else -> RectangleShape
                }

                AccountRow(
                    item = item,
                    showDivider = index > 0 && from == null,
                    onCopy = { onCopyCode(item.code) },
                    onMenu = { sheetFor = item },
                    modifier = Modifier
                        .padding(horizontal = ScreenPadding)
                        .zIndex(if (index == from) 1f else 0f)
                        .graphicsLayer { translationY = shift }
                        .clip(shape)
                        .background(colors.surface)
                        .onSizeChanged { rowHeight = it.height.toFloat() }
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
                                        dragTo = (liveIndex + moved).coerceIn(0, liveLastIndex)
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

        AddAccountButton(
            onClick = { addSheetOpen = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = ScreenPadding, bottom = 24.dp),
        )
    }

    sheetFor?.let { target ->
        OwnOverlay()

        val sheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            onDismissRequest = { sheetFor = null },
            sheetState = sheetState,
            containerColor = colors.surfaceContainerHigh,
            contentColor = colors.onSurface,
        ) {
            AccountSheet(
                item = target,
                onEdit = {
                    sheetFor = null
                    onEditAccount(target.account.id)
                },
                onAdvanceHotp = {
                    sheetFor = null
                    onAdvanceHotp(target.account.id)
                },
                onDelete = {
                    sheetFor = null
                    pendingDelete = target
                },
            )
        }
    }

    if (addSheetOpen) {
        AddAccountSheet(
            onScan = {
                addSheetOpen = false
                onScanAccount()
            },
            onManual = {
                addSheetOpen = false
                onEnterManually()
            },
            onDismiss = { addSheetOpen = false },
        )
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

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(colors.surfaceVariant)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(14.dp).border(1.5.dp, colors.onSurfaceVariant, CircleShape))

        Box(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (query.isEmpty()) {
                Text(
                    "Search",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
            }

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onBackground),
                cursorBrush = SolidColor(colors.onBackground),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) { inner() }
                },
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun PillButton(text: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Box(
        Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(colors.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = colors.onPrimary)
    }
}

@Composable
private fun AddAccountButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(colors.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text("+", style = MaterialTheme.typography.titleLarge, color = colors.onPrimary)
        Text("Add account", style = MaterialTheme.typography.labelLarge, color = colors.onPrimary)
    }
}

@Composable
private fun BackupReminderCard(
    onBackupNow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
            .semantics(mergeDescendants = true) {
                contentDescription = "No backup yet. These secrets exist only on this " +
                    "device and would be lost with it."
            }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("No backup yet", style = MaterialTheme.typography.titleSmall)
        Text(
            "Secrets exist only on this device. Losing it, or changing the screen lock, " +
                "loses every account.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(
                "Back up now",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clickable(onClick = onBackupNow),
            )
            Text(
                "Not now",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onDismiss),
            )
        }
    }
}

@Composable
private fun AccountRow(
    item: AccountCode,
    showDivider: Boolean,
    onCopy: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = spoken(item) }
            .clickable(onClickLabel = "Copy code", onClick = onCopy),
    ) {
        if (showDivider) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant))
        }

        Column(
            Modifier.fillMaxWidth().padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        item.account.issuer.trim().firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurfaceVariant,
                    )
                }

                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        item.account.issuer.uppercase(),
                        style = IssuerLabelStyle,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        codeGroups(item.code).forEach { part ->
                            Text(part, style = CodeStyle, color = colors.onSurface)
                        }
                    }
                    Text(
                        item.account.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable(onClickLabel = "Account options", onClick = onMenu),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "⋮",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.onSurfaceVariant,
                        )
                    }

                    Text(
                        item.secondsRemaining?.let { "${it}s" } ?: "counter",
                        style = TimerStyle,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            val fraction = item.secondsRemaining
                ?.let { it.toFloat() / item.account.periodSeconds }
                ?: 0f

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.surfaceContainerHighest),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.primary),
                )
            }
        }
    }
}

@Composable
private fun AccountSheet(
    item: AccountCode,
    onEdit: () -> Unit,
    onAdvanceHotp: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp)) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
            Text(
                item.account.issuer.uppercase(),
                style = IssuerLabelStyle,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(item.account.label, style = MaterialTheme.typography.titleSmall)
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant))
        Spacer(Modifier.height(8.dp))

        SheetAction(
            glyph = "✎",
            title = "Edit",
            subtitle = "Rename the account or its label",
            onClick = onEdit,
        )

        if (item.account.type == OtpType.HOTP) {
            SheetAction(
                glyph = "↻",
                title = "Next code",
                subtitle = "Counter-based account - advance to the next code",
                onClick = onAdvanceHotp,
            )
        }

        SheetAction(
            glyph = "✕",
            title = "Delete",
            subtitle = "Removes the secret from this device for good",
            onClick = onDelete,
        )

        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun SheetAction(
    glyph: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text(glyph, style = MaterialTheme.typography.titleMedium)
        }

        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountSheet(
    onScan: () -> Unit,
    onManual: () -> Unit,
    onDismiss: () -> Unit,
) {
    OwnOverlay()

    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerHigh,
        contentColor = colors.onSurface,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp)) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                Text("ADD ACCOUNT", style = IssuerLabelStyle, color = colors.onSurfaceVariant)
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant))
            Spacer(Modifier.height(8.dp))

            SheetAction(
                glyph = "\u25A3",
                title = "Scan QR code",
                subtitle = "Point the camera at the code your service shows",
                onClick = onScan,
            )

            SheetAction(
                glyph = "\u270E",
                title = "Enter a setup key",
                subtitle = "Type the issuer and secret by hand",
                onClick = onManual,
            )

            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
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

private fun codeGroups(code: String): List<String> = when (code.length) {
    6 -> listOf(code.substring(0, 3), code.substring(3))
    8 -> listOf(code.substring(0, 4), code.substring(4))
    else -> listOf(code)
}
