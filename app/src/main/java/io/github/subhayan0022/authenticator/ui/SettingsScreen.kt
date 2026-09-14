package io.github.subhayan0022.authenticator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    autoLockSeconds: Int,
    options: List<Int>,
    keyValiditySeconds: Int,
    onAutoLockChange: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Lock after", style = MaterialTheme.typography.titleMedium)

            Text(
                "Codes are hidden and the decryption key becomes unusable once " +
                    "this much time has passed since you authenticated.",
                style = MaterialTheme.typography.bodySmall,
            )

            options.forEach { seconds ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = seconds == autoLockSeconds,
                            onClick = { onAutoLockChange(seconds) },
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = seconds == autoLockSeconds,
                        onClick = { onAutoLockChange(seconds) },
                    )
                    Text(label(seconds), modifier = Modifier.padding(start = 8.dp))
                }
            }

            Text(
                "The longest option is limited to ${label(keyValiditySeconds)} because " +
                    "that is how long this device's encryption key stays usable after " +
                    "one authentication. That limit is fixed when the key is created " +
                    "and cannot be changed without discarding every stored secret.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

private fun label(seconds: Int): String = when {
    seconds < 60 -> "$seconds seconds"
    seconds == 60 -> "1 minute"
    seconds % 60 == 0 -> "${seconds / 60} minutes"
    else -> "$seconds seconds"
}
