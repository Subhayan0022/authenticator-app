package io.github.subhayan0022.authenticator

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.subhayan0022.authenticator.crypto.AppUnlock
import io.github.subhayan0022.authenticator.data.AccountRepository
import io.github.subhayan0022.authenticator.data.DatabaseProvider
import io.github.subhayan0022.authenticator.ui.AccountListScreen
import io.github.subhayan0022.authenticator.ui.AccountListViewModel
import io.github.subhayan0022.authenticator.ui.theme.AuthenticatorTheme

class MainActivity : FragmentActivity() {

    private val viewModel: AccountListViewModel by viewModels {
        AccountListViewModel.factory(
            AccountRepository(DatabaseProvider.get(applicationContext).accountDao()),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AuthenticatorTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AccountListScreen(
                        state = state,
                        onUnlockClick = ::unlock,
                        onAddTestAccount = viewModel::addTestAccount,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    private fun unlock() {
        AppUnlock.prompt(
            activity = this,
            onSuccess = viewModel::onUnlocked,
            onError = { /* stay locked */ },
        )
    }
}
