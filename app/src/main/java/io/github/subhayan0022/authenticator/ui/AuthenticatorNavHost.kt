package io.github.subhayan0022.authenticator.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.subhayan0022.authenticator.data.AccountRepository
import io.github.subhayan0022.authenticator.data.BackupReminder
import io.github.subhayan0022.authenticator.data.LockSettings
import kotlinx.serialization.Serializable

@Serializable
data object AccountListRoute

@Serializable
data object AddAccountRoute

@Serializable
data class EditAccountRoute(val accountId: Long)

@Serializable
data object ScanQrRoute

@Serializable
data object SettingsRoute

@Serializable
data class BackupRoute(val importing: Boolean)

@Composable
fun AuthenticatorNavHost(
    repository: AccountRepository,
    lockSettings: LockSettings,
    backupReminder: BackupReminder,
    listState: AccountListUiState,
    onDeleteAccount: (Long) -> Unit,
    onMoveAccount: (Long, Int) -> Unit,
    onAdvanceHotp: (Long) -> Unit,
    onCopyCode: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onGroupSelected: (String?) -> Unit,
    onCreateGroup: (String) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onUnlockRequest: (onSuccess: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val groupsFlow = remember(repository) { repository.observeGroups() }
    val groups by groupsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    NavHost(
        navController = navController,
        startDestination = AccountListRoute,
        modifier = modifier,
    ) {
        composable<AccountListRoute> {
            SecureScreen()

            val shouldRemind by backupReminder.shouldRemind.collectAsStateWithLifecycle()

            AccountListScreen(
                state = listState,
                onUnlockClick = { onUnlockRequest {} },
                onScanAccount = { navController.navigate(ScanQrRoute) },
                onEnterManually = { navController.navigate(AddAccountRoute) },
                onDelete = onDeleteAccount,
                onEditAccount = { navController.navigate(EditAccountRoute(it)) },
                onMove = onMoveAccount,
                onAdvanceHotp = onAdvanceHotp,
                onCopyCode = onCopyCode,
                onQueryChange = onQueryChange,
                onGroupSelected = onGroupSelected,
                onCreateGroup = onCreateGroup,
                onRenameGroup = onRenameGroup,
                onDeleteGroup = onDeleteGroup,
                onSettingsClick = { navController.navigate(SettingsRoute) },
                showBackupReminder = shouldRemind,
                onBackupNow = { navController.navigate(BackupRoute(importing = false)) },
                onDismissReminder = backupReminder::dismissForNow,
            )
        }

        composable<AddAccountRoute> {
            val addViewModel: AddAccountViewModel =
                viewModel(factory = AddAccountViewModel.factory(repository))
            val form by addViewModel.form.collectAsStateWithLifecycle()

            SecureScreen()

            AddAccountScreen(
                form = form,
                groups = groups,
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

        composable<ScanQrRoute> {
            SecureScreen()

            val scanViewModel: AddAccountViewModel =
                viewModel(factory = AddAccountViewModel.factory(repository))
            val scanForm by scanViewModel.form.collectAsStateWithLifecycle()

            ScanQrScreen(
                form = scanForm,
                groups = groups,
                onScanned = scanViewModel::applyScannedUri,
                onGroupChange = scanViewModel::onGroupChange,
                onSave = { scanViewModel.save { navController.popBackStack() } },
                onUnlockAndSave = {
                    onUnlockRequest { scanViewModel.save { navController.popBackStack() } }
                },
                onRescan = scanViewModel::reset,
                onBack = { navController.popBackStack() },
            )
        }

        composable<SettingsRoute> {
            val strict by lockSettings.strictMode.collectAsStateWithLifecycle()
            val timeout by lockSettings.idleTimeoutSeconds.collectAsStateWithLifecycle()

            SettingsScreen(
                strictMode = strict,
                timeoutSeconds = timeout,
                options = lockSettings.optionsFor(strict),
                keyValiditySeconds = lockSettings.optionsFor(strict = true).last(),
                onStrictModeChange = lockSettings::setStrictMode,
                onTimeoutChange = lockSettings::setIdleTimeoutSeconds,
                onExportClick = { navController.navigate(BackupRoute(importing = false)) },
                onImportClick = { navController.navigate(BackupRoute(importing = true)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable<BackupRoute> { entry ->
            SecureScreen()

            val route: BackupRoute = entry.toRoute()
            val context = LocalContext.current

            val backupViewModel: BackupViewModel =
                viewModel(factory = BackupViewModel.factory(repository, backupReminder))
            val backupState by backupViewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(route.importing) {
                backupViewModel.setMode(
                    if (route.importing) BackupMode.IMPORT else BackupMode.EXPORT,
                )
            }

            BackupScreen(
                state = backupState,
                onPasswordChange = backupViewModel::onPasswordChange,
                onConfirmPasswordChange = backupViewModel::onConfirmPasswordChange,
                onExport = { uri ->
                    backupViewModel.export(context, uri) { navController.popBackStack() }
                },
                onImport = { uri ->
                    backupViewModel.import(context, uri) { navController.popBackStack() }
                },
                onUnlock = {
                    onUnlockRequest {
                        backupViewModel.retry(context) { navController.popBackStack() }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable<EditAccountRoute> { entry ->
            SecureScreen()

            val route: EditAccountRoute = entry.toRoute()

            val editViewModel: EditAccountViewModel =
                viewModel(factory = EditAccountViewModel.factory(repository, route.accountId))
            val form by editViewModel.form.collectAsStateWithLifecycle()

            EditAccountScreen(
                form = form,
                groups = groups,
                onIssuerChange = editViewModel::onIssuerChange,
                onLabelChange = editViewModel::onLabelChange,
                onGroupChange = editViewModel::onGroupChange,
                onSave = { editViewModel.save { navController.popBackStack() } },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
