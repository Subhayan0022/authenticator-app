package io.github.subhayan0022.authenticator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkScheme = darkColorScheme(
    primary = DarkContent,
    onPrimary = DarkBackground,
    secondary = DarkContentMuted,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkContent,
    surface = DarkSurface,
    onSurface = DarkContent,
    surfaceVariant = DarkField,
    onSurfaceVariant = DarkContentMuted,
    surfaceContainerHigh = DarkSheet,
    outline = DarkOutline,
    outlineVariant = DarkDivider,
    surfaceContainerHighest = DarkTrack,
    error = DarkError,
    onError = DarkBackground,
)

private val LightScheme = lightColorScheme(
    primary = LightContent,
    onPrimary = LightBackground,
    secondary = LightContentMuted,
    onSecondary = LightBackground,
    background = LightBackground,
    onBackground = LightContent,
    surface = LightSurface,
    onSurface = LightContent,
    surfaceVariant = LightField,
    onSurfaceVariant = LightContentMuted,
    surfaceContainerHigh = LightSheet,
    outline = LightOutline,
    outlineVariant = LightDivider,
    surfaceContainerHighest = LightTrack,
    error = LightError,
    onError = LightBackground,
)

@Composable
fun AuthenticatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = Typography,
        content = content,
    )
}
