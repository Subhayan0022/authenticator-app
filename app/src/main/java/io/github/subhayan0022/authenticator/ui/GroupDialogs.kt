package io.github.subhayan0022.authenticator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.subhayan0022.authenticator.data.isReservedGroup
import io.github.subhayan0022.authenticator.ui.theme.CardCorner
import io.github.subhayan0022.authenticator.ui.theme.RowInset

private const val ReservedMessage = "\"All\" is the built-in filter and cannot be used."

@Composable
fun GroupManagerSheet(
    groups: List<String>,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    var renaming by remember { mutableStateOf<String?>(null) }
    var removing by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }

    val reserved: (String) -> String? = { if (isReservedGroup(it)) ReservedMessage else null }

    renaming?.let { original ->
        PromptSheet(
            label = "Rename group",
            title = original,
            fieldLabel = "Group name",
            initial = original,
            placeholder = "Work",
            confirmLabel = "Rename",
            hint = "Using an existing name merges the two groups.",
            validate = reserved,
            onConfirm = {
                onRename(original, it)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
        return
    }

    removing?.let { name ->
        ConfirmSheet(
            label = "Remove group",
            title = "Remove $name?",
            body = "The group disappears and its accounts become ungrouped. " +
                "No accounts or codes are deleted.",
            confirmLabel = "Remove group",
            destructive = true,
            onConfirm = {
                onDelete(name)
                removing = null
            },
            onDismiss = { removing = null },
        )
        return
    }

    if (creating) {
        PromptSheet(
            label = "New group",
            title = "Create a group",
            fieldLabel = "Group name",
            initial = "",
            placeholder = "Work",
            confirmLabel = "Create",
            validate = reserved,
            onConfirm = {
                onCreate(it)
                creating = false
            },
            onDismiss = { creating = false },
        )
        return
    }

    AppSheet(onDismiss = onDismiss) {
        SheetHeading(label = "Groups", title = "Organise your accounts")

        Spacer(Modifier.height(28.dp))

        if (groups.isEmpty()) {
            Text(
                "No groups yet. Create one, then pick it when you add or edit an account.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CardCorner))
                    .background(colors.surface),
            ) {
                groups.forEachIndexed { index, group ->
                    if (index > 0) FieldDivider()

                    GroupRow(
                        name = group,
                        onRename = { renaming = group },
                        onRemove = { removing = group },
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        PrimaryButton(
            label = "New group",
            enabled = true,
            onClick = { creating = true },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(4.dp))

        SheetDismiss(label = "Done", onClick = onDismiss)
    }
}

@Composable
private fun GroupRow(name: String, onRename: () -> Unit, onRemove: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = RowInset, end = 8.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )

        GroupAction(label = "Rename", color = colors.onSurfaceVariant, onClick = onRename)
        GroupAction(label = "Remove", color = colors.error, onClick = onRemove)
    }
}

@Composable
private fun GroupAction(label: String, color: Color, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    )
}
