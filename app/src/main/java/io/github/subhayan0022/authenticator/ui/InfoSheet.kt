package io.github.subhayan0022.authenticator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.subhayan0022.authenticator.ui.theme.IssuerLabelStyle

data class InfoSection(val heading: String, val body: String)

@Composable
fun InfoSheet(
    title: String,
    sections: List<InfoSection>,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    AppSheet(onDismiss = onDismiss) {
        Text(
            title.uppercase(),
            style = IssuerLabelStyle,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            sections.forEach { section ->
                Column {
                    Text(section.heading, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        section.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}
