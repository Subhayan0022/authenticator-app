package io.github.subhayan0022.authenticator.data

import io.github.subhayan0022.authenticator.crypto.KeystoreSecretCipher
import io.github.subhayan0022.authenticator.crypto.SecretCipher
import io.github.subhayan0022.authenticator.otp.HotpGenerator
import io.github.subhayan0022.authenticator.otp.TotpGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountRepository(
    private val dao: AccountDao,
    private val cipher: SecretCipher = KeystoreSecretCipher,
) {

    fun observeAccounts(): Flow<List<Account>> =
        dao.observeAll().map { entities -> entities.map { it.toAccount() } }

    /** Encrypts and stores [secret]. The caller's array is zeroed. */
    suspend fun add(
        issuer: String,
        label: String,
        secret: ByteArray,
        groupName: String? = null,
        type: OtpType = OtpType.TOTP,
        algorithm: String = "HmacSHA1",
        digits: Int = 6,
        periodSeconds: Int = TotpGenerator.DEFAULT_PERIOD_SECONDS,
    ): Long = try {
        dao.insert(
            AccountEntity(
                issuer = issuer,
                label = label,
                groupName = groupName,
                secret = cipher.encrypt(secret),
                type = type,
                algorithm = algorithm,
                digits = digits,
                periodSeconds = periodSeconds,
            ),
        )
    } finally {
        secret.fill(0)
    }

    /** Decrypts, generates, and discards. Returns null if the account is gone. */
    suspend fun currentCode(
        id: Long,
        timeMillis: Long = System.currentTimeMillis(),
    ): String? {
        val entity = dao.findById(id) ?: return null
        val secret = cipher.decrypt(entity.secret)

        return try {
            when (entity.type) {
                OtpType.TOTP -> TotpGenerator.generate(
                    secret, timeMillis, entity.periodSeconds, entity.digits, entity.algorithm,
                )

                OtpType.HOTP -> HotpGenerator.generate(
                    secret, entity.counter, entity.digits, entity.algorithm,
                )
            }
        } finally {
            secret.fill(0)
        }
    }

    /** Consumes the next HOTP counter value. Each call yields a different code. */
    suspend fun advanceHotp(id: Long): String? {
        val counter = dao.nextCounter(id) ?: return null
        val entity = dao.findById(id) ?: return null
        val secret = cipher.decrypt(entity.secret)

        return try {
            HotpGenerator.generate(secret, counter, entity.digits, entity.algorithm)
        } finally {
            secret.fill(0)
        }
    }

    suspend fun updateMetadata(
        id: Long,
        issuer: String,
        label: String,
        groupName: String?,
        sortOrder: Int,
    ) {
        val entity = dao.findById(id) ?: return
        dao.update(
            entity.copy(
                issuer = issuer,
                label = label,
                groupName = groupName,
                sortOrder = sortOrder,
            ),
        )
    }

    suspend fun delete(id: Long) {
        dao.findById(id)?.let { dao.delete(it) }
    }
}
