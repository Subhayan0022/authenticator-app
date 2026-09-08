package io.github.subhayan0022.authenticator

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.subhayan0022.authenticator.crypto.AppUnlock
import io.github.subhayan0022.authenticator.data.AccountRepository
import io.github.subhayan0022.authenticator.data.DatabaseProvider
import io.github.subhayan0022.authenticator.ui.AuthenticatorNavHost
import io.github.subhayan0022.authenticator.ui.AccountListViewModel
import io.github.subhayan0022.authenticator.ui.theme.AuthenticatorTheme

class MainActivity : FragmentActivity() {

    private val repository by lazy {
        AccountRepository(DatabaseProvider.get(applicationContext).accountDao())
    }

    private val viewModel: AccountListViewModel by viewModels {
        AccountListViewModel.factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AuthenticatorTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                AuthenticatorNavHost(
                    repository = repository,
                    listState = state,
                    onUnlockRequest = ::unlock,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    private fun unlock(onSuccess: () -> Unit) {
        AppUnlock.prompt(
            activity = this,
            onSuccess = {
                viewModel.onUnlocked()
                onSuccess()
            },
            onError = { /* stay locked */ },
        )
    }
}
