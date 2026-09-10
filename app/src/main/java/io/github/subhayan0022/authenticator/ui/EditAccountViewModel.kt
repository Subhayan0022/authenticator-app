package io.github.subhayan0022.authenticator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.subhayan0022.authenticator.data.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditAccountFormState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val issuer: String = "",
    val label: String = "",
    val group: String = "",
    val issuerError: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
) {
    val canSave: Boolean
        get() = !loading && !saving && issuer.isNotBlank() && issuerError == null
}

class EditAccountViewModel(
    private val repository: AccountRepository,
    private val accountId: Long,
) : ViewModel() {

    private val _form = MutableStateFlow(EditAccountFormState())
    val form: StateFlow<EditAccountFormState> = _form.asStateFlow()

    /** Carried through untouched so renaming never changes list position. */
    private var sortOrder: Int = 0

    init {
        viewModelScope.launch {
            val account = repository.account(accountId)

            if (account == null) {
                _form.update { it.copy(loading = false, missing = true) }
            } else {
                sortOrder = account.sortOrder
                _form.update {
                    it.copy(
                        loading = false,
                        issuer = account.issuer,
                        label = account.label,
                        group = account.groupName.orEmpty(),
                    )
                }
            }
        }
    }

    fun onIssuerChange(value: String) = _form.update {
        it.copy(
            issuer = value,
            issuerError = if (value.isBlank()) "Issuer is required" else null,
        )
    }

    fun onLabelChange(value: String) = _form.update { it.copy(label = value) }

    fun onGroupChange(value: String) = _form.update { it.copy(group = value) }

    fun save(onSaved: () -> Unit) {
        val current = _form.value
        if (!current.canSave) return

        _form.update { it.copy(saving = true, saveError = null) }

        viewModelScope.launch {
            try {
                repository.updateMetadata(
                    id = accountId,
                    issuer = current.issuer.trim(),
                    label = current.label.trim(),
                    groupName = current.group.trim().ifBlank { null },
                    sortOrder = sortOrder,
                )
                onSaved()
            } catch (e: Exception) {
                _form.update {
                    it.copy(saving = false, saveError = e.message ?: "Could not save changes")
                }
            }
        }
    }

    companion object {
        fun factory(
            repository: AccountRepository,
            accountId: Long,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { EditAccountViewModel(repository, accountId) }
        }
    }
}
