package io.github.subhayan0022.authenticator.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.subhayan0022.authenticator.data.LockSettings
import io.github.subhayan0022.authenticator.ui.theme.IssuerLabelStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ScreenPadding = 20.dp
private val CardCorner = 14.dp
private val RowInset = 18.dp
private const val ConfirmDelayMillis = 320L

@Composable
fun SettingsScreen(
    strictMode: Boolean,
    timeoutSeconds: Int,
    options: List<Int>,
    keyValiditySeconds: Int,
    onStrictModeChange: (Boolean) -> Unit,
    onTimeoutChange: (Int) -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var pickerOpen by remember { mutableStateOf(false) }
    var strictInfoOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.background,
        contentColor = colors.onBackground,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            Header(onBack = onBack)

            SectionLabel("SECURITY")

            SettingsCard {
                ToggleRow(
                    title = "Strict mode",
                    subtitle = "Never keeps secrets in memory. Locks on a fixed timer.",
                    checked = strictMode,
                    onCheckedChange = onStrictModeChange,
                    onInfoClick = { strictInfoOpen = true },
                )

                RowDivider()

                ActionRow(
                    title = "Auto-lock",
                    value = timeoutLabel(timeoutSeconds),
                    onClick = { pickerOpen = true },
                )
            }

            Footnote("Always locks the moment you leave the app.")

            SectionLabel("BACKUP")

            SettingsCard {
                ActionRow(title = "Export", onClick = onExportClick)
                RowDivider()
                ActionRow(title = "Import", onClick = onImportClick)
            }

            Spacer(Modifier.height(28.dp))
        }
    }

    if (pickerOpen) {
        TimeoutPicker(
            strictMode = strictMode,
            selected = timeoutSeconds,
            options = options,
            keyValiditySeconds = keyValiditySeconds,
            onSelect = onTimeoutChange,
            onDismiss = { pickerOpen = false },
        )
    }

    if (strictInfoOpen) {
        InfoSheet(
            title = "Strict mode",
            sections = StrictModeInfo,
            onDismiss = { strictInfoOpen = false },
        )
    }
}

private val StrictModeInfo = listOf(
    InfoSection(
        heading = "What it does",
        body = "Secrets are decrypted only for the instant a code is generated, then " +
            "wiped. Nothing sensitive sits in memory while the app is open.",
    ),
    InfoSection(
        heading = "The trade-off",
        body = "The timer starts the moment you authenticate and cannot be extended by " +
            "using the app. It locks on schedule, whatever you are in the middle of.",
    ),
    InfoSection(
        heading = "Why the options are shorter",
        body = "Your device's encryption key stays usable for a fixed time after one " +
            "authentication. The timer cannot outlast it, so the longer choices may " +
            "not be offered.",
    ),
)

@Composable
private fun Header(onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp)) {
        Box(
            Modifier
                .padding(start = 6.dp)
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .semantics { contentDescription = "Back" },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "←",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            "Settings",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = ScreenPadding, top = 2.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = IssuerLabelStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = ScreenPadding,
            end = ScreenPadding,
            top = 18.dp,
            bottom = 10.dp,
        ),
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding)
            .clip(RoundedCornerShape(CardCorner))
            .background(MaterialTheme.colorScheme.surface),
        content = content,
    )
}

@Composable
private fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = RowInset)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onInfoClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
            .padding(start = RowInset, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)

                if (onInfoClick != null) {
                    InfoButton(
                        description = "About $title",
                        onClick = onInfoClick,
                    )
                }
            }

            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary,
                checkedBorderColor = colors.primary,
                uncheckedThumbColor = colors.onSurfaceVariant,
                uncheckedTrackColor = colors.surfaceContainerHighest,
                uncheckedBorderColor = colors.outline,
            ),
        )
    }
}

@Composable
private fun InfoButton(description: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .requiredSize(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick, role = Role.Button)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(1.dp, colors.onSurfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "i",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                    ),
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActionRow(title: String, value: String? = null, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = RowInset, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )

        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }

        Text(
            "›",
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun Footnote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 10.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeoutPicker(
    strictMode: Boolean,
    selected: Int,
    options: List<Int>,
    keyValiditySeconds: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    OwnOverlay()

    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    var pending by remember { mutableStateOf(selected) }
    var closing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerHigh,
        contentColor = colors.onSurface,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp)) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                Text("AUTO-LOCK", style = IssuerLabelStyle, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (strictMode) {
                        "Counted from your last unlock."
                    } else {
                        "Counted from your last tap."
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant))
            Spacer(Modifier.height(8.dp))

            options.forEach { seconds ->
                OptionRow(
                    label = timeoutLabel(seconds),
                    selected = seconds == pending,
                    onClick = {
                        if (!closing) {
                            closing = true
                            pending = seconds
                            onSelect(seconds)

                            scope.launch {
                                delay(ConfirmDelayMillis)
                                sheetState.hide()
                                onDismiss()
                            }
                        }
                    },
                )
            }

            if (strictMode && keyValiditySeconds < LockSettings.STRICT_OPTIONS.last()) {
                Text(
                    "Capped at ${timeoutLabel(keyValiditySeconds)} by this device's key.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp),
                )
            }

            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    val fill by animateColorAsState(
        targetValue = if (selected) colors.surfaceContainerHighest else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "optionFill",
    )

    val content by animateColorAsState(
        targetValue = if (selected) colors.onSurface else colors.onSurfaceVariant,
        animationSpec = tween(durationMillis = 150),
        label = "optionContent",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = content,
            modifier = Modifier.weight(1f),
        )

        if (selected) {
            Text("✓", style = MaterialTheme.typography.titleMedium, color = content)
        }
    }
}

private fun timeoutLabel(seconds: Int): String = when {
    seconds < 60 -> "$seconds seconds"
    seconds == 60 -> "1 minute"
    seconds % 60 == 0 -> "${seconds / 60} minutes"
    else -> "$seconds seconds"
}
