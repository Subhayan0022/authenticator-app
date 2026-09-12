package io.github.subhayan0022.authenticator.ui

import android.security.keystore.UserNotAuthenticatedException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.subhayan0022.authenticator.data.AccountRepository
import io.github.subhayan0022.authenticator.data.OtpType
import io.github.subhayan0022.authenticator.otp.Base32
import io.github.subhayan0022.authenticator.otp.OtpAuthType
import io.github.subhayan0022.authenticator.otp.OtpAuthUri
import io.github.subhayan0022.authenticator.otp.TotpGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddAccountFormState(
    val issuer: String = "",
    val label: String = "",
    val group: String = "",
    val secret: String = "",
    val issuerError: String? = null,
    val secretError: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
    val needsAuth: Boolean = false,
    val type: OtpType = OtpType.TOTP,
    val algorithm: String = "HmacSHA1",
    val digits: Int = 6,
    val periodSeconds: Int = TotpGenerator.DEFAULT_PERIOD_SECONDS,
    val counter: Long = 0,
    val scanned: Boolean = false,
) {
    val canSave: Boolean
        get() = !saving &&
            issuer.isNotBlank() &&
            secret.isNotBlank() &&
            issuerError == null &&
            secretError == null
}

class AddAccountViewModel(
    private val repository: AccountRepository,
) : ViewModel() {

    private val _form = MutableStateFlow(AddAccountFormState())
    val form: StateFlow<AddAccountFormState> = _form.asStateFlow()

    fun onIssuerChange(value: String) = _form.update {
        it.copy(
            issuer = value,
            issuerError = if (value.isBlank()) "Issuer is required" else null,
        )
    }

    fun onLabelChange(value: String) = _form.update { it.copy(label = value) }

    fun onGroupChange(value: String) = _form.update { it.copy(group = value) }

    fun onSecretChange(value: String) = _form.update {
        it.copy(secret = value, secretError = validateSecret(value))
    }

    fun save(onSaved: () -> Unit) {
        val current = _form.value
        if (!current.canSave) return

        _form.update { it.copy(saving = true, saveError = null, needsAuth = false) }

        viewModelScope.launch {
            try {
                repository.add(
                    issuer = current.issuer.trim(),
                    label = current.label.trim(),
                    secret = Base32.decode(current.secret),
                    groupName = current.group.trim().ifBlank { null },
                    type = current.type,
                    algorithm = current.algorithm,
                    digits = current.digits,
                    periodSeconds = current.periodSeconds,
                    counter = current.counter,
                )
                onSaved()
            } catch (e: UserNotAuthenticatedException) {
                _form.update {
                    it.copy(
                        saving = false,
                        needsAuth = true,
                        saveError = "Locked - authenticate to save",
                    )
                }
            } catch (e: Exception) {
                _form.update {
                    it.copy(saving = false, saveError = e.message ?: "Could not save account")
                }
            }
        }
    }

    /** Fills the form from a scanned otpauth link, or reports why it was rejected. */
    fun applyScannedUri(raw: String) {
        val data = try {
            OtpAuthUri.parse(raw)
        } catch (e: OtpAuthUri.InvalidUriException) {
            _form.update { it.copy(saveError = e.message ?: "That QR code is not an otpauth link") }
            return
        }

        // The form works from the Base32 text; the decoded copy is not needed here.
        data.secret.fill(0)

        _form.update {
            it.copy(
                issuer = data.issuer,
                label = data.label,
                secret = data.secretBase32,
                issuerError = if (data.issuer.isBlank()) "Issuer is required" else null,
                secretError = null,
                saveError = null,
                type = when (data.type) {
                    OtpAuthType.TOTP -> OtpType.TOTP
                    OtpAuthType.HOTP -> OtpType.HOTP
                },
                algorithm = data.algorithm,
                digits = data.digits,
                periodSeconds = data.periodSeconds,
                counter = data.counter,
                scanned = true,
            )
        }
    }

    /** Returns an error message, or null if the secret is valid or not yet typed. */
    private fun validateSecret(raw: String): String? {
        if (raw.isBlank()) return null

        return try {
            Base32.decode(raw).fill(0)
            null
        } catch (e: Base32.InvalidBase32Exception) {
            e.message ?: "Not a valid Base32 secret"
        }
    }

    companion object {
        fun factory(repository: AccountRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { AddAccountViewModel(repository) }
        }
    }
}
