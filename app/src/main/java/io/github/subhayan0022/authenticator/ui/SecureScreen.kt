package io.github.subhayan0022.authenticator.ui

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * Blocks screenshots, screen recording and the recent-apps thumbnail while the
 * calling composable is on screen. Applied per screen rather than once on the
 * Activity so non-sensitive screens stay screenshotable.
 */
@Composable
fun SecureScreen() {
    val activity = LocalActivity.current

    DisposableEffect(activity) {
        val window = activity?.window
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
