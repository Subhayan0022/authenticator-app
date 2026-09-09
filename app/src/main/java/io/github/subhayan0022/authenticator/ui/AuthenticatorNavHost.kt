package io.github.subhayan0022.authenticator.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.subhayan0022.authenticator.data.AccountRepository
import kotlinx.serialization.Serializable

@Serializable
data object AccountListRoute

@Serializable
data object AddAccountRoute

@Composable
fun AuthenticatorNavHost(
    repository: AccountRepository,
    listState: AccountListUiState,
    onDeleteAccount: (Long) -> Unit,
    onUnlockRequest: (onSuccess: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AccountListRoute,
        modifier = modifier,
    ) {
        composable<AccountListRoute> {
            AccountListScreen(
                state = listState,
                onUnlockClick = { onUnlockRequest {} },
                onAddAccountClick = { navController.navigate(AddAccountRoute) },
                onDelete = onDeleteAccount,
            )
        }

        composable<AddAccountRoute> {
            val addViewModel: AddAccountViewModel =
                viewModel(factory = AddAccountViewModel.factory(repository))
            val form by addViewModel.form.collectAsStateWithLifecycle()

            AddAccountScreen(
                form = form,
                onIssuerChange = addViewModel::onIssuerChange,
                onLabelChange = addViewModel::onLabelChange,
                onGroupChange = addViewModel::onGroupChange,
                onSecretChange = addViewModel::onSecretChange,
                onSave = { addViewModel.save { navController.popBackStack() } },
                onUnlockAndSave = {
                    onUnlockRequest { addViewModel.save { navController.popBackStack() } }
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
