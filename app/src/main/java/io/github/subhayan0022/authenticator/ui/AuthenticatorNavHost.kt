package io.github.subhayan0022.authenticator.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.subhayan0022.authenticator.data.AccountRepository
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

private const val SCANNED_URI = "scannedUri"

@Composable
fun AuthenticatorNavHost(
    repository: AccountRepository,
    lockSettings: LockSettings,
    listState: AccountListUiState,
    onDeleteAccount: (Long) -> Unit,
    onMoveAccount: (Long, Int) -> Unit,
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
            SecureScreen()

            AccountListScreen(
                state = listState,
                onUnlockClick = { onUnlockRequest {} },
                onAddAccountClick = { navController.navigate(AddAccountRoute) },
                onDelete = onDeleteAccount,
                onEditAccount = { navController.navigate(EditAccountRoute(it)) },
                onMove = onMoveAccount,
                onCopyCode = onCopyCode,
                onSettingsClick = { navController.navigate(SettingsRoute) },
            )
        }

        composable<AddAccountRoute> { entry ->
            val addViewModel: AddAccountViewModel =
                viewModel(factory = AddAccountViewModel.factory(repository))
            val form by addViewModel.form.collectAsStateWithLifecycle()

            SecureScreen()

            val scanned by entry.savedStateHandle
                .getStateFlow<String?>(SCANNED_URI, null)
                .collectAsStateWithLifecycle()

            LaunchedEffect(scanned) {
                scanned?.let { raw ->
                    addViewModel.applyScannedUri(raw)
                    entry.savedStateHandle[SCANNED_URI] = null
                }
            }

            AddAccountScreen(
                form = form,
                onIssuerChange = addViewModel::onIssuerChange,
                onLabelChange = addViewModel::onLabelChange,
                onGroupChange = addViewModel::onGroupChange,
                onSecretChange = addViewModel::onSecretChange,
                onSave = { addViewModel.save { navController.popBackStack() } },
                onScanClick = { navController.navigate(ScanQrRoute) },
                onUnlockAndSave = {
                    onUnlockRequest { addViewModel.save { navController.popBackStack() } }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable<ScanQrRoute> {
            ScanQrScreen(
                onQrCode = { text ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(SCANNED_URI, text)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable<SettingsRoute> {
            val autoLock by lockSettings.autoLockSeconds.collectAsStateWithLifecycle()

            SettingsScreen(
                autoLockSeconds = autoLock,
                options = lockSettings.options,
                keyValiditySeconds = lockSettings.options.last(),
                onAutoLockChange = lockSettings::setAutoLockSeconds,
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
                viewModel(factory = BackupViewModel.factory(repository))
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
                onUnlock = { onUnlockRequest {} },
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
                onIssuerChange = editViewModel::onIssuerChange,
                onLabelChange = editViewModel::onLabelChange,
                onGroupChange = editViewModel::onGroupChange,
                onSave = { editViewModel.save { navController.popBackStack() } },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
