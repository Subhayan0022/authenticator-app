package io.github.subhayan0022.authenticator.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.subhayan0022.authenticator.ui.theme.ScreenPadding

@Composable
fun BackupScreen(
    state: BackupUiState,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onExport: (Uri) -> Unit,
    onImport: (Uri) -> Unit,
    onUnlock: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    val createFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> uri?.let(onExport) }

    val openFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(onImport) }

    val exporting = state.mode == BackupMode.EXPORT
    val mismatch = exporting && state.confirmPassword.isNotEmpty() && !state.passwordsMatch

    val plainKeyboard = KeyboardOptions(capitalization = KeyboardCapitalization.None)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.background,
        contentColor = colors.onBackground,
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenTopBar(
                title = if (exporting) "Export backup" else "Import backup",
                onBack = onBack,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                SectionLabel("ENCRYPTION")

                FormCard {
                    FieldRow(
                        label = "Password",
                        value = state.password,
                        onValueChange = onPasswordChange,
                        placeholder = "At least ${BackupUiState.MIN_PASSWORD_LENGTH} characters",
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = plainKeyboard,
                    )

                    if (exporting) {
                        FieldDivider()

                        FieldRow(
                            label = "Confirm password",
                            value = state.confirmPassword,
                            onValueChange = onConfirmPasswordChange,
                            placeholder = "Type it again",
                            error = if (mismatch) "Passwords do not match" else null,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = plainKeyboard,
                        )
                    }
                }

                Footnote(
                    if (exporting) {
                        "This password is the only thing protecting the file. If you forget " +
                            "it, nobody can recover the backup — including you. Keep it " +
                            "somewhere safe and separate."
                    } else {
                        "Imported accounts are added alongside your existing ones. Nothing " +
                            "is overwritten or removed."
                    },
                )

                if (state.busy) {
                    Row(
                        modifier = Modifier.padding(
                            start = ScreenPadding,
                            end = ScreenPadding,
                            top = 24.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = colors.onSurfaceVariant,
                            strokeWidth = 2.dp,
                        )

                        Text(
                            "Deriving the key — slow on purpose.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }

                state.message?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(
                            start = ScreenPadding,
                            end = ScreenPadding,
                            top = 24.dp,
                        ),
                    )
                }

                Spacer(Modifier.height(28.dp))
            }

            PrimaryBar(
                label = when {
                    state.busy -> "Working…"
                    state.needsAuth -> "Unlock and continue"
                    exporting -> "Choose where to save"
                    else -> "Choose a backup file"
                },
                enabled = state.canSubmit,
                onClick = {
                    when {
                        state.needsAuth -> onUnlock()
                        exporting -> createFile.launch("lockstep-backup.totpbak")
                        else -> openFile.launch(arrayOf("*/*"))
                    }
                },
                error = state.error,
            )
        }
    }
}
