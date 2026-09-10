package io.github.subhayan0022.authenticator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountScreen(
    form: EditAccountFormState,
    onIssuerChange: (String) -> Unit,
    onLabelChange: (String) -> Unit,
    onGroupChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Edit account") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Cancel") }
                },
            )
        },
    ) { innerPadding ->
        when {
            form.loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            form.missing -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("This account no longer exists.")
                    Button(onClick = onBack) { Text("Back") }
                }
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = form.issuer,
                    onValueChange = onIssuerChange,
                    label = { Text("Issuer") },
                    singleLine = true,
                    isError = form.issuerError != null,
                    supportingText = form.issuerError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = form.label,
                    onValueChange = onLabelChange,
                    label = { Text("Account") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = form.group,
                    onValueChange = onGroupChange,
                    label = { Text("Group (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    "The secret cannot be changed. To use a different secret, " +
                        "delete this account and add it again.",
                    style = MaterialTheme.typography.bodySmall,
                )

                form.saveError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = onSave,
                    enabled = form.canSave,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (form.saving) "Saving..." else "Save changes")
                }
            }
        }
    }
}
