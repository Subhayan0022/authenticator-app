package io.github.subhayan0022.authenticator.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    state: BackupUiState,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onExport: (android.net.Uri) -> Unit,
    onImport: (android.net.Uri) -> Unit,
    onUnlock: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val createFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> uri?.let(onExport) }

    val openFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(onImport) }

    val exporting = state.mode == BackupMode.EXPORT

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (exporting) "Export backup" else "Import backup") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                if (exporting) {
                    "Your accounts are encrypted with this password alone. " +
                        "If you forget it, the backup cannot be recovered by anyone, " +
                        "including you. Store it somewhere safe and separate."
                } else {
                    "Imported accounts are added alongside your existing ones. " +
                        "Nothing is overwritten or removed."
                },
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                supportingText = {
                    Text("At least ${BackupUiState.MIN_PASSWORD_LENGTH} characters")
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (exporting) {
                OutlinedTextField(
                    value = state.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Confirm password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = state.confirmPassword.isNotEmpty() && !state.passwordsMatch,
                    supportingText = {
                        if (state.confirmPassword.isNotEmpty() && !state.passwordsMatch) {
                            Text("Passwords do not match")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            state.message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            if (state.busy) {
                CircularProgressIndicator()
                Text(
                    "Deriving the key. This is deliberately slow.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = {
                    when {
                        state.needsAuth -> onUnlock()
                        exporting -> createFile.launch("authenticator-backup.totpbak")
                        else -> openFile.launch(arrayOf("*/*"))
                    }
                },
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        state.needsAuth -> "Unlock and continue"
                        exporting -> "Choose where to save"
                        else -> "Choose a backup file"
                    },
                )
            }
        }
    }
}
