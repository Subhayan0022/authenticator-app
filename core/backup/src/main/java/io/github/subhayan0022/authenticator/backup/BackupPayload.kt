package io.github.subhayan0022.authenticator.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupAccount(
    val issuer: String,
    val label: String,
    val secretBase32: String,
    val type: String = "TOTP",
    val algorithm: String = "HmacSHA1",
    val digits: Int = 6,
    val periodSeconds: Int = 30,
    val counter: Long = 0,
    val group: String? = null,
    val sortOrder: Int = 0,
)

@Serializable
data class BackupPayload(
    val version: Int = CURRENT_VERSION,
    val accounts: List<BackupAccount>,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}
