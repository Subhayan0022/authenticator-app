package io.github.subhayan0022.authenticator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.subhayan0022.authenticator.ui.theme.CardCorner
import io.github.subhayan0022.authenticator.ui.theme.IssuerLabelStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    OwnOverlay()

    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerHigh,
        contentColor = colors.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            content = content,
        )
    }
}

@Composable
fun SheetHeading(label: String, title: String) {
    Text(
        label.uppercase(),
        style = IssuerLabelStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(10.dp))

    Text(title, style = MaterialTheme.typography.titleLarge)
}

@Composable
fun SheetDismiss(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
    )
}

@Composable
fun ConfirmSheet(
    label: String,
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AppSheet(onDismiss = onDismiss) {
        SheetHeading(label = label, title = title)

        Spacer(Modifier.height(12.dp))

        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(28.dp))

        PrimaryButton(
            label = confirmLabel,
            enabled = true,
            onClick = onConfirm,
            destructive = destructive,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(4.dp))

        SheetDismiss(label = "Cancel", onClick = onDismiss)
    }
}

@Composable
fun PromptSheet(
    label: String,
    title: String,
    fieldLabel: String,
    initial: String,
    placeholder: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    hint: String? = null,
    validate: (String) -> String? = { null },
) {
    var value by remember { mutableStateOf(initial) }
    val error = validate(value)

    AppSheet(onDismiss = onDismiss) {
        SheetHeading(label = label, title = title)

        Spacer(Modifier.height(18.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CardCorner))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            FieldRow(
                label = fieldLabel,
                value = value,
                onValueChange = { value = it },
                placeholder = placeholder,
                error = error,
                hint = hint,
            )
        }

        Spacer(Modifier.height(24.dp))

        PrimaryButton(
            label = confirmLabel,
            enabled = value.isNotBlank() && error == null,
            onClick = { onConfirm(value) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(4.dp))

        SheetDismiss(label = "Cancel", onClick = onDismiss)
    }
}
