package io.github.subhayan0022.authenticator

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import io.github.subhayan0022.authenticator.crypto.AppUnlock
import io.github.subhayan0022.authenticator.crypto.KeystoreSecretCipher
import io.github.subhayan0022.authenticator.otp.Base32
import io.github.subhayan0022.authenticator.otp.TotpGenerator
import io.github.subhayan0022.authenticator.ui.theme.AuthenticatorTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val activity = this

        setContent {
            AuthenticatorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CryptoTestScreen(
                        activity = activity,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Composable
fun CryptoTestScreen(activity: FragmentActivity, modifier: Modifier = Modifier) {
    var status by remember { mutableStateOf("Not unlocked yet") }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(status)

        Button(
            onClick = {
                AppUnlock.prompt(
                    activity = activity,
                    onSuccess = { status = runCryptoRoundTrip() },
                    onError = { error -> status = "Unlock failed: $error" },
                )
            },
        ) {
            Text("Unlock and test crypto")
        }
    }
}

private fun runCryptoRoundTrip(): String = try {
    val original = Base32.decode("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")

    val encrypted = KeystoreSecretCipher.encrypt(original)
    val decrypted = KeystoreSecretCipher.decrypt(encrypted)

    val now = System.currentTimeMillis()

    buildString {
        appendLine("Round trip OK: ${decrypted.contentEquals(original)}")
        appendLine("Ciphertext: ${encrypted.ciphertext.size} bytes")
        appendLine("IV: ${encrypted.iv.size} bytes")
        appendLine("Code: ${TotpGenerator.generate(decrypted, now)}")
        appendLine("Expires in: ${TotpGenerator.secondsRemaining(now)}s")
    }
} catch (e: Exception) {
    "FAILED: ${e::class.simpleName}: ${e.message}"
}
