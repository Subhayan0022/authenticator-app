package io.github.subhayan0022.authenticator.ui

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

private var secureScreenCount = 0

private const val SCREENSHOT_CAPTURE_MODE = true

private fun acquireSecure(activity: Activity?) {
    if (SCREENSHOT_CAPTURE_MODE) return
    val window = activity?.window ?: return
    if (secureScreenCount++ == 0) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
    }
}

private fun releaseSecure(activity: Activity?) {
    if (SCREENSHOT_CAPTURE_MODE) return
    val window = activity?.window ?: return
    if (--secureScreenCount <= 0) {
        secureScreenCount = 0
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

@Composable
fun SecureScreen() {
    val activity = LocalActivity.current

    DisposableEffect(activity) {
        acquireSecure(activity)
        onDispose { releaseSecure(activity) }
    }
}
