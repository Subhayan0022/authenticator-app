package io.github.subhayan0022.authenticator.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SecureClipboard(context: Context) {

    private val manager = context.applicationContext
        .getSystemService(ClipboardManager::class.java)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var clearJob: Job? = null

    fun copyCode(code: String) {
        val clipboard = manager ?: return

        clipboard.setPrimaryClip(sensitiveClip(code))

        clearJob?.cancel()
        clearJob = scope.launch {
            delay(AUTO_CLEAR_MILLIS)
            clearIfUnchanged(code)
        }
    }

    private fun sensitiveClip(code: String): ClipData =
        ClipData.newPlainText(CLIP_LABEL, code).apply {
            description.extras = PersistableBundle().apply {
                putBoolean(sensitiveKey(), true)
            }
        }

    private fun clearIfUnchanged(code: String) {
        val clipboard = manager ?: return

        val current = clipboard.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.text
            ?.toString()

        if (current == code) {
            clipboard.clearPrimaryClip()
        }
    }

    private fun sensitiveKey(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ClipDescription.EXTRA_IS_SENSITIVE
        } else {
            LEGACY_SENSITIVE_KEY
        }

    companion object {
        private const val CLIP_LABEL = "One-time code"
        private const val AUTO_CLEAR_MILLIS = 30_000L

        /** The pre-API-33 name of the same flag, honoured by several OEM skins. */
        private const val LEGACY_SENSITIVE_KEY = "android.content.extra.IS_SENSITIVE"
    }
}
