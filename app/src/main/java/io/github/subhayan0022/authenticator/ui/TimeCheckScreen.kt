package io.github.subhayan0022.authenticator.ui

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.subhayan0022.authenticator.data.ClockCheck
import io.github.subhayan0022.authenticator.data.ClockStatus
import io.github.subhayan0022.authenticator.data.DriftLevel
import io.github.subhayan0022.authenticator.ui.theme.ScreenPadding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val ClockFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

@Composable
fun TimeCheckScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    var status by remember { mutableStateOf(ClockCheck.run(context)) }

    val unverifiedOffline = status is ClockStatus.Unverified &&
        !(status as ClockStatus.Unverified).automaticTimeOn

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.background,
        contentColor = colors.onBackground,
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenTopBar(title = "Time check", onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                SectionLabel("RESULT")

                when (val current = status) {
                    is ClockStatus.Measured -> MeasuredResult(current)
                    is ClockStatus.Unverified -> UnverifiedResult(current)
                }

                Footnote(
                    "Codes are generated from the clock on this device. If it drifts far " +
                        "enough, the codes are still correct for the wrong moment and the " +
                        "service rejects them.",
                )

                Spacer(Modifier.height(28.dp))
            }

            PrimaryBar(
                label = if (unverifiedOffline) "Open date & time settings" else "Check again",
                enabled = true,
                onClick = {
                    if (unverifiedOffline) {
                        context.startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
                    } else {
                        status = ClockCheck.run(context)
                    }
                },
            )
        }
    }
}

@Composable
private fun MeasuredResult(status: ClockStatus.Measured) {
    val colors = MaterialTheme.colorScheme
    val level = ClockCheck.levelFor(status.offsetMillis)

    val headline = when (level) {
        DriftLevel.ACCURATE -> "Your clock is accurate"
        DriftLevel.MINOR -> "Your clock is slightly off"
        DriftLevel.SEVERE -> "Your clock is wrong"
    }

    val body = when (level) {
        DriftLevel.ACCURATE -> "Codes generated here line up with what services expect."
        DriftLevel.MINOR ->
            "Codes should still be accepted, but the margin is smaller than it should be."
        DriftLevel.SEVERE ->
            "Codes are likely to be rejected. Turn on automatic date and time to fix it."
    }

    ResultCard(
        headline = headline,
        body = body,
        tone = if (level == DriftLevel.SEVERE) colors.error else colors.onSurface,
    )

    SectionLabel("DETAILS")

    FormCard {
        DetailRow("This device", ClockFormat.format(Instant.ofEpochMilli(status.deviceMillis)))
        FieldDivider()
        DetailRow("Network time", ClockFormat.format(Instant.ofEpochMilli(status.networkMillis)))
        FieldDivider()
        DetailRow("Difference", describeOffset(status.offsetMillis))
    }
}

@Composable
private fun UnverifiedResult(status: ClockStatus.Unverified) {
    if (status.automaticTimeOn) {
        ResultCard(
            headline = "Automatic time is on",
            body = "This device keeps its own clock in sync, so codes will line up. " +
                "There is no separate network reading to compare against right now.",
            tone = MaterialTheme.colorScheme.onSurface,
        )
    } else {
        ResultCard(
            headline = "Cannot check right now",
            body = "Automatic date and time is switched off and this device has no network " +
                "time to compare against. Turning it on is the reliable fix.",
            tone = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ResultCard(headline: String, body: String, tone: androidx.compose.ui.graphics.Color) {
    FormCard {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text(headline, style = MaterialTheme.typography.titleLarge, color = tone)
            Spacer(Modifier.height(10.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun describeOffset(millis: Long): String {
    val magnitude = abs(millis)

    val amount = when {
        magnitude < 1_000 -> "$magnitude ms"
        magnitude < 60_000 -> String.format(Locale.US, "%.1f s", magnitude / 1_000.0)
        else -> "${magnitude / 60_000} min ${(magnitude % 60_000) / 1_000} s"
    }

    return when {
        magnitude < 1_000 -> amount
        millis > 0 -> "$amount ahead"
        else -> "$amount behind"
    }
}
