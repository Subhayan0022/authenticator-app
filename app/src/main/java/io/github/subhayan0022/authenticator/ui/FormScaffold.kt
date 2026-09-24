package io.github.subhayan0022.authenticator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.subhayan0022.authenticator.ui.theme.CardCorner
import io.github.subhayan0022.authenticator.ui.theme.IssuerLabelStyle
import io.github.subhayan0022.authenticator.ui.theme.PlexMono
import io.github.subhayan0022.authenticator.ui.theme.RowInset
import io.github.subhayan0022.authenticator.ui.theme.ScreenPadding

@Composable
fun ScreenTopBar(title: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 18.dp)) {
        Box(
            Modifier
                .padding(start = 6.dp)
                .size(46.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .semantics { contentDescription = "Back" },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "←",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = ScreenPadding, top = 2.dp),
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = IssuerLabelStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = ScreenPadding,
            end = ScreenPadding,
            top = 26.dp,
            bottom = 12.dp,
        ),
    )
}

@Composable
fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding)
            .clip(RoundedCornerShape(CardCorner))
            .background(MaterialTheme.colorScheme.surface),
        content = content,
    )
}

@Composable
fun FieldDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = RowInset)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
fun Footnote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 12.dp),
    )
}

@Composable
fun FieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    hint: String? = null,
    mono: Boolean = false,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = MaterialTheme.colorScheme

    val textStyle = MaterialTheme.typography.bodyLarge.let {
        if (mono) it.copy(fontFamily = PlexMono) else it
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RowInset, vertical = 17.dp),
    ) {
        Text(label.uppercase(), style = IssuerLabelStyle, color = colors.onSurfaceVariant)

        Spacer(Modifier.height(9.dp))

        Box {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = textStyle,
                    color = colors.onSurfaceVariant.copy(alpha = 0.45f),
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = textStyle.copy(
                    color = if (enabled) colors.onSurface else colors.onSurfaceVariant,
                ),
                cursorBrush = SolidColor(colors.onSurface),
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val support = error ?: hint

        if (support != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                support,
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) colors.error else colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PrimaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme

    val container = when {
        !enabled -> colors.surfaceContainerHighest
        destructive -> colors.error
        else -> colors.primary
    }

    val content = when {
        !enabled -> colors.onSurfaceVariant
        destructive -> colors.onError
        else -> colors.onPrimary
    }

    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(container)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

@Composable
fun PrimaryBar(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(start = ScreenPadding, end = ScreenPadding, top = 14.dp, bottom = 18.dp),
    ) {
        if (error != null) {
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        PrimaryButton(
            label = label,
            enabled = enabled,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun AppSpinner(modifier: Modifier = Modifier, size: Dp = 26.dp) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        strokeWidth = size / 10,
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme

    Column(
        Modifier.fillMaxWidth().padding(horizontal = RowInset, vertical = 15.dp),
    ) {
        Text(label.uppercase(), style = IssuerLabelStyle, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
