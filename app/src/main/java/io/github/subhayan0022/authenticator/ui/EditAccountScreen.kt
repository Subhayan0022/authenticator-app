package io.github.subhayan0022.authenticator.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.subhayan0022.authenticator.ui.theme.ScreenPadding

@Composable
fun EditAccountScreen(
    form: EditAccountFormState,
    groups: List<String>,
    onIssuerChange: (String) -> Unit,
    onLabelChange: (String) -> Unit,
    onGroupChange: (String) -> Unit,
    onSave: () -> Unit,
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
            ScreenTopBar(title = "Edit account", onBack = onBack)

            when {
                form.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    AppSpinner()
                }

                form.missing -> Column(
                    modifier = Modifier.fillMaxSize().padding(ScreenPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(80.dp))

                    Text(
                        "This account no longer exists.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(20.dp))

                    PrimaryButton(
                        label = "Back",
                        enabled = true,
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                else -> {
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
                            )
                        }

                        Footnote(
                            "The secret cannot be changed. To use a different one, " +
                                "delete this account and add it again.",
                        )

                        SectionLabel("GROUP")

                        GroupPicker(
                            groups = groups,
                            selected = form.group,
                            onSelect = onGroupChange,
                        )

                        Spacer(Modifier.height(28.dp))
                    }

                    PrimaryBar(
                        label = if (form.saving) "Saving…" else "Save changes",
                        enabled = form.canSave,
                        onClick = onSave,
                        error = form.saveError,
                    )
                }
            }
        }
    }
}
