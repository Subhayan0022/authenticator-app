package io.github.subhayan0022.authenticator.ui

import android.security.keystore.UserNotAuthenticatedException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.subhayan0022.authenticator.data.Account
import io.github.subhayan0022.authenticator.data.AccountRepository
import io.github.subhayan0022.authenticator.data.OtpType
import io.github.subhayan0022.authenticator.otp.TotpGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class AccountCode(
    val account: Account,
    val code: String,
    val secondsRemaining: Int?,
)

sealed interface AccountListUiState {
    data object Loading : AccountListUiState
    data object Locked : AccountListUiState
    data class Ready(val codes: List<AccountCode>) : AccountListUiState
}

class AccountListViewModel(
    private val repository: AccountRepository,
) : ViewModel() {

    private val unlocked = MutableStateFlow(false)

    /** accountId -> (time step or counter, code) — the code is cached, the secret never is. */
    private val codeCache = mutableMapOf<Long, Pair<Long, String>>()

    private val tick = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000)
        }
    }

    val uiState: StateFlow<AccountListUiState> =
        combine(unlocked, repository.observeAccounts(), tick) { isUnlocked, accounts, now ->
            if (!isUnlocked) return@combine AccountListUiState.Locked

            try {
                AccountListUiState.Ready(accounts.map { codeFor(it, now) })
            } catch (e: UserNotAuthenticatedException) {
                relock()
                AccountListUiState.Locked
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AccountListUiState.Loading,
        )

    fun onUnlocked() {
        unlocked.value = true
    }

    fun relock() {
        codeCache.clear()
        unlocked.value = false
    }

    private suspend fun codeFor(account: Account, now: Long): AccountCode {
        val step = when (account.type) {
            OtpType.TOTP -> now / 1_000 / account.periodSeconds
            OtpType.HOTP -> account.counter
        }

        val cached = codeCache[account.id]
        val code = if (cached != null && cached.first == step) {
            cached.second
        } else {
            (repository.currentCode(account.id, now) ?: "??????")
                .also { codeCache[account.id] = step to it }
        }

        return AccountCode(
            account = account,
            code = code,
            secondsRemaining = when (account.type) {
                OtpType.TOTP -> TotpGenerator.secondsRemaining(now, account.periodSeconds)
                OtpType.HOTP -> null
            },
        )
    }

    companion object {
        fun factory(repository: AccountRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { AccountListViewModel(repository) }
        }
    }
}
