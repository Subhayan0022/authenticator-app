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

    private val _strictMode = MutableStateFlow(prefs.getBoolean(KEY_STRICT_MODE, false))
    val strictMode: StateFlow<Boolean> = _strictMode.asStateFlow()

    private val _idleTimeoutSeconds = MutableStateFlow(
        clamp(prefs.getInt(KEY_IDLE_TIMEOUT, DEFAULT_IDLE_SECONDS), _strictMode.value),
    )
    val idleTimeoutSeconds: StateFlow<Int> = _idleTimeoutSeconds.asStateFlow()

    fun optionsFor(strict: Boolean): List<Int> = if (strict) {
        STRICT_OPTIONS.filter { it <= keyValiditySeconds }.ifEmpty { listOf(keyValiditySeconds) }
    } else {
        RELAXED_OPTIONS
    }

    fun setStrictMode(strict: Boolean) {
        prefs.edit().putBoolean(KEY_STRICT_MODE, strict).apply()
        _strictMode.value = strict
        setIdleTimeoutSeconds(_idleTimeoutSeconds.value)
    }

    fun setIdleTimeoutSeconds(seconds: Int) {
        val value = clamp(seconds, _strictMode.value)
        prefs.edit().putInt(KEY_IDLE_TIMEOUT, value).apply()
        _idleTimeoutSeconds.value = value
    }

    private fun clamp(seconds: Int, strict: Boolean): Int {
        val options = optionsFor(strict)
        return options.minByOrNull { kotlin.math.abs(it - seconds) } ?: options.first()
    }

    companion object {
        private const val PREFS_NAME = "lock_settings"
        private const val KEY_STRICT_MODE = "strict_mode"
        private const val KEY_IDLE_TIMEOUT = "idle_timeout_seconds"

        const val DEFAULT_IDLE_SECONDS = 300

        val STRICT_OPTIONS = listOf(15, 30, 60, 120, 300)
        val RELAXED_OPTIONS = listOf(60, 120, 300, 600, 1_800)
    }
}
