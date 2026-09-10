package io.github.subhayan0022.authenticator.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.subhayan0022.authenticator.data.AccountRepository
import kotlinx.serialization.Serializable

@Serializable
data object AccountListRoute

@Serializable
data object AddAccountRoute

@Serializable
data class EditAccountRoute(val accountId: Long)

@Composable
fun AuthenticatorNavHost(
    repository: AccountRepository,
    listState: AccountListUiState,
    onDeleteAccount: (Long) -> Unit,
    onCopyCode: (String) -> Unit,
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
                onEditAccount = { navController.navigate(EditAccountRoute(it)) },
                onCopyCode = onCopyCode,
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

        composable<EditAccountRoute> { entry ->
            val route: EditAccountRoute = entry.toRoute()

            val editViewModel: EditAccountViewModel =
                viewModel(factory = EditAccountViewModel.factory(repository, route.accountId))
            val form by editViewModel.form.collectAsStateWithLifecycle()

            EditAccountScreen(
                form = form,
                onIssuerChange = editViewModel::onIssuerChange,
                onLabelChange = editViewModel::onLabelChange,
                onGroupChange = editViewModel::onGroupChange,
                onSave = { editViewModel.save { navController.popBackStack() } },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
