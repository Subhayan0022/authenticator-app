package io.github.subhayan0022.authenticator.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BackupReminder(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var dismissedThisSession = false

    private val _shouldRemind = MutableStateFlow(!prefs.getBoolean(KEY_BACKED_UP, false))
    val shouldRemind: StateFlow<Boolean> = _shouldRemind.asStateFlow()

    fun markBackedUp(){
        prefs.edit().putBoolean(KEY_BACKED_UP, true).apply()
        _shouldRemind.value = false
    }

    fun dismissForNow() {
        dismissedThisSession = true
        _shouldRemind.value = false
    }

    companion object {
        private const val PREFS_NAME = "backup_reminder"
        private const val KEY_BACKED_UP = "has_backed_up"
    }
}