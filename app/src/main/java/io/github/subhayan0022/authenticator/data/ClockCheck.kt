package io.github.subhayan0022.authenticator.data

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import java.time.DateTimeException
import kotlin.math.abs

sealed interface ClockStatus {

    data class Measured(val deviceMillis: Long, val networkMillis: Long) : ClockStatus {
        val offsetMillis: Long get() = deviceMillis - networkMillis
    }

    data class Unverified(val automaticTimeOn: Boolean) : ClockStatus
}

enum class DriftLevel { ACCURATE, MINOR, SEVERE }

object ClockCheck {

    private const val MINOR_THRESHOLD_MILLIS = 2_000L
    private const val SEVERE_THRESHOLD_MILLIS = 15_000L

    fun run(context: Context): ClockStatus {
        val network = networkTimeMillis()
            ?: return ClockStatus.Unverified(automaticTimeOn(context))

        return ClockStatus.Measured(System.currentTimeMillis(), network)
    }

    fun levelFor(offsetMillis: Long): DriftLevel = when (abs(offsetMillis)) {
        in 0 until MINOR_THRESHOLD_MILLIS -> DriftLevel.ACCURATE
        in MINOR_THRESHOLD_MILLIS until SEVERE_THRESHOLD_MILLIS -> DriftLevel.MINOR
        else -> DriftLevel.SEVERE
    }

    private fun networkTimeMillis(): Long? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null

        return try {
            SystemClock.currentNetworkTimeClock().millis()
        } catch (e: DateTimeException) {
            null
        }
    }

    private fun automaticTimeOn(context: Context): Boolean =
        Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME, 0) == 1
}
