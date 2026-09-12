package io.github.subhayan0022.authenticator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import io.github.subhayan0022.authenticator.data.OtpType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    form: AddAccountFormState,
    onIssuerChange: (String) -> Unit,
    onLabelChange: (String) -> Unit,
    onGroupChange: (String) -> Unit,
    onSecretChange: (String) -> Unit,
    onSave: () -> Unit,
    onScanClick: () -> Unit,
    onUnlockAndSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Add account") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Cancel") }
                },
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
            OutlinedButton(
                onClick = onScanClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Scan QR code")
            }

            OutlinedTextField(
                value = form.issuer,
                onValueChange = onIssuerChange,
                label = { Text("Issuer") },
                placeholder = { Text("GitHub") },
                singleLine = true,
                isError = form.issuerError != null,
                supportingText = form.issuerError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.label,
                onValueChange = onLabelChange,
                label = { Text("Account") },
                placeholder = { Text("alice@example.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.group,
                onValueChange = onGroupChange,
                label = { Text("Group (optional)") },
                placeholder = { Text("Work") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.secret,
                onValueChange = onSecretChange,
                label = { Text("Secret key") },
                placeholder = { Text("JBSWY3DPEHPK3PXP") },
                singleLine = true,
                isError = form.secretError != null,
                supportingText = form.secretError?.let { { Text(it) } }
                    ?: { Text("Base32 — spaces, dashes and case are ignored") },
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            if (form.scanned) {
                Text(
                    "From QR code: ${form.type}, ${form.algorithm.removePrefix("Hmac")}, " +
                        "${form.digits} digits, " +
                        if (form.type == OtpType.HOTP) {
                            "counter ${form.counter}"
                        } else {
                            "${form.periodSeconds}s period"
                        },
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            form.saveError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = if (form.needsAuth) onUnlockAndSave else onSave,
                enabled = form.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        form.saving -> "Saving..."
                        form.needsAuth -> "Unlock and save"
                        else -> "Save"
                    },
                )
            }
        }
    }
}
