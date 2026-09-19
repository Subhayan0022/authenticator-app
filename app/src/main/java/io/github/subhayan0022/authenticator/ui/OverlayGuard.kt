package io.github.subhayan0022.authenticator.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

object OverlayGuard {

    private var count = 0

    val isShowing: Boolean
        get() = count > 0

    fun enter() {
        count++
    }

    fun exit() {
        count = (count - 1).coerceAtLeast(0)
    }
}

@Composable
fun OwnOverlay() {
    DisposableEffect(Unit) {
        OverlayGuard.enter()
        onDispose { OverlayGuard.exit() }
    }
}
