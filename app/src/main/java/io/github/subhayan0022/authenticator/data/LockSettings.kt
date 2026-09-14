package io.github.subhayan0022.authenticator.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LockSettings(
    context: Context,
    private val keyValiditySeconds: Int,
) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _autoLockSeconds = MutableStateFlow(clamp(stored()))
    val autoLockSeconds: StateFlow<Int> = _autoLockSeconds.asStateFlow()

    /** Only timeouts the current key can honour - longer ones would expire mid-session. */
    val options: List<Int> = OPTIONS
        .filter { it <= keyValiditySeconds }
        .ifEmpty { listOf(keyValiditySeconds) }

    fun setAutoLockSeconds(seconds: Int) {
        val value = clamp(seconds)
        prefs.edit().putInt(KEY_AUTO_LOCK, value).apply()
        _autoLockSeconds.value = value
    }

    private fun stored(): Int = prefs.getInt(KEY_AUTO_LOCK, DEFAULT_SECONDS)

    private fun clamp(seconds: Int): Int = seconds
        .coerceAtLeast(OPTIONS.first())
        .coerceAtMost(keyValiditySeconds)

    companion object {
        private const val PREFS_NAME = "lock_settings"
        private const val KEY_AUTO_LOCK = "auto_lock_seconds"

        const val DEFAULT_SECONDS = 60
        val OPTIONS = listOf(15, 30, 60, 120, 300)
    }
}
