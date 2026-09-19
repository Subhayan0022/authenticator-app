package io.github.subhayan0022.authenticator

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.subhayan0022.authenticator.crypto.AppUnlock
import io.github.subhayan0022.authenticator.data.AccountRepository
import io.github.subhayan0022.authenticator.data.DatabaseProvider
import io.github.subhayan0022.authenticator.data.BackupReminder
import io.github.subhayan0022.authenticator.data.LockSettings
import io.github.subhayan0022.authenticator.crypto.KeystoreSecretCipher
import io.github.subhayan0022.authenticator.ui.AuthenticatorNavHost
import io.github.subhayan0022.authenticator.ui.OverlayGuard
import io.github.subhayan0022.authenticator.ui.SecureClipboard
import io.github.subhayan0022.authenticator.ui.AccountListViewModel
import io.github.subhayan0022.authenticator.ui.theme.AuthenticatorTheme

class MainActivity : FragmentActivity() {

    private val repository by lazy {
        AccountRepository(
            DatabaseProvider.get(applicationContext).accountDao(),
            DatabaseProvider.get(applicationContext).groupDao(),
        )
    }

    private val clipboard by lazy { SecureClipboard(this) }

    private val backupReminder by lazy { BackupReminder(this) }

    private val lockSettings by lazy {
        LockSettings(this, KeystoreSecretCipher.keyValiditySeconds())
    }

    private val viewModel: AccountListViewModel by viewModels {
        AccountListViewModel.factory(repository, lockSettings)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val fromShortcut = intent?.getBooleanExtra(EXTRA_PROMPT_UNLOCK, false) == true

        setContent {
            AuthenticatorTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                AuthenticatorNavHost(
                    repository = repository,
                    lockSettings = lockSettings,
                    backupReminder = backupReminder,
                    listState = state,
                    onDeleteAccount = viewModel::delete,
                    onMoveAccount = viewModel::move,
                    onAdvanceHotp = viewModel::advanceHotp,
                    onCopyCode = clipboard::copyCode,
                    onQueryChange = viewModel::onQueryChange,
                    onGroupSelected = viewModel::onGroupSelected,
                    onCreateGroup = viewModel::createGroup,
                    onRenameGroup = viewModel::renameGroup,
                    onDeleteGroup = viewModel::deleteGroup,
                    onUnlockRequest = ::unlock,
                    modifier = Modifier.fillMaxSize(),
                )

                LaunchedEffect(fromShortcut) {
                    if (fromShortcut) unlock {}
                }
            }
        }
    }

    companion object {
        const val EXTRA_PROMPT_UNLOCK = "promptUnlock"
    }

    private var authInProgress = false

    override fun onResume() {
        super.onResume()
        authInProgress = false
    }

    override fun onPause() {
        super.onPause()
        if (!authInProgress) viewModel.onBackgrounded()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && !authInProgress && !OverlayGuard.isShowing) {
            viewModel.onBackgrounded()
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        viewModel.onInteraction()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onBackgrounded()
    }

    private fun unlock(onSuccess: () -> Unit) {
        authInProgress = true

        AppUnlock.prompt(
            activity = this,
            onSuccess = {
                authInProgress = false
                viewModel.onUnlocked()
                onSuccess()
            },
            onError = { authInProgress = false },
        )
    }
}
