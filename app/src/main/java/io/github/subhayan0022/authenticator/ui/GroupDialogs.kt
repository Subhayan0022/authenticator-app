package io.github.subhayan0022.authenticator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GroupManagerDialog(
    groups: List<String>,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    OwnOverlay()

    var newName by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }

    renaming?.let { original ->
        RenameDialog(
            original = original,
            onDismiss = { renaming = null },
            onConfirm = {
                onRename(original, it)
                renaming = null
            },
        )
        return
    }

    confirmDelete?.let { name ->
        OwnOverlay()

        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Remove $name?") },
            text = {
                Text(
                    "The group disappears and its accounts become ungrouped. " +
                        "No accounts or codes are deleted.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(name)
                        confirmDelete = null
                    },
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Groups") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (groups.isEmpty()) {
                    Text(
                        "No groups yet. Create one below, then assign accounts to it " +
                            "from their Edit screen.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                groups.forEach { group ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(group, modifier = Modifier.weight(1f))
                        TextButton(onClick = { renaming = group }) { Text("Rename") }
                        TextButton(onClick = { confirmDelete = group }) { Text("Remove") }
                    }
                }

                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New group") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )

                TextButton(
                    onClick = {
                        onCreate(newName)
                        newName = ""
                    },
                    enabled = newName.isNotBlank(),
                ) {
                    Text("Create")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

@Composable
private fun RenameDialog(
    original: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    OwnOverlay()

    var name by remember { mutableStateOf(original) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename $original") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Group name") },
                singleLine = true,
                supportingText = { Text("Using an existing name merges the two groups.") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
