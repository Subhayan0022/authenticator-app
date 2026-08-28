package io.github.subhayan0022.authenticator.crypto

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object AppUnlock {
    private const val AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun canAuthenticate(context: Context): Int =
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS)

    fun prompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (CharSequence) -> Unit,
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) = onSuccess()

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) = onError(errString)
            },
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Authenticator")
            .setSubtitle("Confirm it's you to view your codes")
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()

        prompt.authenticate(info)
    }
}
