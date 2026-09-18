package io.github.subhayan0022.authenticator.ui

import android.security.keystore.UserNotAuthenticatedException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.subhayan0022.authenticator.data.Account
import io.github.subhayan0022.authenticator.data.AccountRepository
import io.github.subhayan0022.authenticator.data.LockSettings
import io.github.subhayan0022.authenticator.data.OtpType
import io.github.subhayan0022.authenticator.otp.TotpGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private val lockSettings: LockSettings,
) : ViewModel() {

    private data class LockState(
        val unlockedAt: Long = 0L,
        val lastInteractionAt: Long = 0L,
    )

    private val lockState = MutableStateFlow(LockState())

    /** accountId -> (time step or counter, code) — the code is cached, the secret never is. */
    private val codeCache = mutableMapOf<Long, Pair<Long, String>>()

    init {
        viewModelScope.launch {
            lockSettings.strictMode.collect { strict ->
                if (strict) relock()
            }
        }
    }

    private val tick = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000)
        }
    }

    val uiState: StateFlow<AccountListUiState> =
        combine(
            lockState,
            repository.observeAccounts(),
            tick,
            lockSettings.idleTimeoutSeconds,
        ) { lock, accounts, now, timeoutSeconds ->
            if (lock.unlockedAt == 0L) return@combine AccountListUiState.Locked

            val since = if (lockSettings.strictMode.value) {
                lock.unlockedAt
            } else {
                lock.lastInteractionAt
            }

            if (now - since >= timeoutSeconds * 1_000L) {
                relock()
                return@combine AccountListUiState.Locked
            }

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
        val now = System.currentTimeMillis()
        lockState.value = LockState(unlockedAt = now, lastInteractionAt = now)

        if (!lockSettings.strictMode.value) {
            viewModelScope.launch {
                try {
                    repository.openSession()
                } catch (e: UserNotAuthenticatedException) {
                    relock()
                }
            }
        }
    }

    fun onInteraction() {
        val now = System.currentTimeMillis()
        lockState.update { if (it.unlockedAt == 0L) it else it.copy(lastInteractionAt = now) }
    }

    fun onBackgrounded() {
        if (lockState.value.unlockedAt != 0L) relock()
    }

    fun move(id: Long, offset: Int) {
        viewModelScope.launch {
            repository.move(id, offset)
        }
    }

    fun advanceHotp(id: Long) {
        viewModelScope.launch {
            try {
                repository.advanceHotp(id)
            } catch (e: UserNotAuthenticatedException) {
                relock()
            }
        }
    }

    fun delete(id: Long){
        viewModelScope.launch {
            codeCache.remove(id)
            repository.delete(id)
        }
    }

    fun relock() {
        codeCache.clear()
        repository.closeSession()
        lockState.value = LockState()
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
        fun factory(
            repository: AccountRepository,
            lockSettings: LockSettings,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { AccountListViewModel(repository, lockSettings) }
        }
    }
}
