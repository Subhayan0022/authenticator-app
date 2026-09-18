package io.github.subhayan0022.authenticator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Locking", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Strict mode", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Secrets are decrypted only for the instant a code is made, " +
                            "never held in memory. The trade-off is that the timer runs " +
                            "from when you authenticated and cannot be extended by using " +
                            "the app, and it cannot exceed the key's own limit.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = strictMode, onCheckedChange = onStrictModeChange)
            }

            Text(
                if (strictMode) "Lock after authenticating" else "Lock after no interaction",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp),
            )

            Text(
                if (strictMode) {
                    "Counted from your fingerprint, regardless of what you are doing."
                } else {
                    "Counted from the last time you touched the app."
                },
                style = MaterialTheme.typography.bodySmall,
            )

            options.forEach { seconds ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = seconds == timeoutSeconds,
                            onClick = { onTimeoutChange(seconds) },
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = seconds == timeoutSeconds,
                        onClick = { onTimeoutChange(seconds) },
                    )
                    Text(label(seconds), modifier = Modifier.padding(start = 8.dp))
                }
            }

            Text(
                "The app always locks the moment you leave it, whichever mode is on.",
                style = MaterialTheme.typography.bodySmall,
            )

            if (strictMode) {
                Text(
                    "Strict mode is capped at ${label(keyValiditySeconds)} because that is how " +
                        "long this device's encryption key stays usable after one " +
                        "authentication. That limit is fixed when the key is created.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Text(
                "Backup",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp),
            )

            Text(
                "Your secrets cannot leave this device on their own. Without a backup, " +
                    "losing the device or changing the screen lock loses every account.",
                style = MaterialTheme.typography.bodySmall,
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = onExportClick) { Text("Export") }
                OutlinedButton(onClick = onImportClick) { Text("Import") }
            }
        }
    }
}

private fun label(seconds: Int): String = when {
    seconds < 60 -> "$seconds seconds"
    seconds == 60 -> "1 minute"
    seconds % 60 == 0 -> "${seconds / 60} minutes"
    else -> "$seconds seconds"
}
