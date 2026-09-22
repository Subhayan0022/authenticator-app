package io.github.subhayan0022.authenticator.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.subhayan0022.authenticator.data.OtpType
import io.github.subhayan0022.authenticator.ui.theme.ScreenPadding

@Composable
fun AddAccountScreen(
    form: AddAccountFormState,
    groups: List<String>,
    onIssuerChange: (String) -> Unit,
    onLabelChange: (String) -> Unit,
    onGroupChange: (String) -> Unit,
    onSecretChange: (String) -> Unit,
    onSave: () -> Unit,
    onUnlockAndSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.background,
        contentColor = colors.onBackground,
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenTopBar(title = "Add account", onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                SectionLabel("ACCOUNT")

                FormCard {
                    FieldRow(
                        label = "Issuer",
                        value = form.issuer,
                        onValueChange = onIssuerChange,
                        placeholder = "GitHub",
                        error = form.issuerError,
                    )

                    FieldDivider()

                    FieldRow(
                        label = "Account",
                        value = form.label,
                        onValueChange = onLabelChange,
                        placeholder = "alice@example.com",
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                        ),
                    )

                    FieldDivider()

                    FieldRow(
                        label = "Secret key",
                        value = form.secret,
                        onValueChange = onSecretChange,
                        placeholder = "JBSWY3DPEHPK3PXP",
                        error = form.secretError,
                        hint = if (form.scanned) {
                            scannedSummary(form)
                        } else {
                            "Base32 — spaces, dashes and case are ignored"
                        },
                        mono = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done,
                        ),
                    )
                }

                SectionLabel("GROUP")

                GroupPicker(
                    groups = groups,
                    selected = form.group,
                    onSelect = onGroupChange,
                )

                Spacer(Modifier.height(28.dp))
            }

            PrimaryBar(
                label = when {
                    form.saving -> "Saving…"
                    form.needsAuth -> "Unlock and save"
                    else -> "Save"
                },
                enabled = form.canSave,
                onClick = if (form.needsAuth) onUnlockAndSave else onSave,
                error = form.saveError,
            )
        }
    }
}

@Composable
fun GroupPicker(
    groups: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    var creating by remember { mutableStateOf(selected.isNotBlank() && selected !in groups) }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Pill(
                text = "None",
                selected = !creating && selected.isBlank(),
                onClick = {
                    creating = false
                    onSelect("")
                },
            )

            groups.forEach { group ->
                Pill(
                    text = group,
                    selected = !creating && selected == group,
                    onClick = {
                        creating = false
                        onSelect(group)
                    },
                )
            }

            Pill(
                text = "+ New",
                selected = creating,
                onClick = {
                    if (!creating) {
                        creating = true
                        onSelect("")
                    }
                },
            )
        }

        if (creating) {
            Spacer(Modifier.height(12.dp))

            FormCard {
                FieldRow(
                    label = "New group",
                    value = selected,
                    onValueChange = onSelect,
                    placeholder = "Work",
                )
            }
        }
    }
}

private fun scannedSummary(form: AddAccountFormState): String {
    val cadence = if (form.type == OtpType.HOTP) {
        "counter ${form.counter}"
    } else {
        "${form.periodSeconds}s"
    }

    return "From QR · ${form.type} · ${form.algorithm.removePrefix("Hmac")} · " +
        "${form.digits} digits · $cadence"
}
