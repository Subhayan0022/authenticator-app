package io.github.subhayan0022.authenticator.data

import androidx.compose.ui.geometry.Offset
import io.github.subhayan0022.authenticator.crypto.KeystoreSecretCipher
import io.github.subhayan0022.authenticator.crypto.SecretCipher
import io.github.subhayan0022.authenticator.backup.BackupAccount
import io.github.subhayan0022.authenticator.backup.BackupPayload
import io.github.subhayan0022.authenticator.otp.Base32
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
        counter: Long = 0,
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
                counter = counter,
                sortOrder = dao.allOrdered().size,
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

    suspend fun account(id: Long): Account? = dao.findById(id)?.toAccount()

    suspend fun exportPayload(): BackupPayload = BackupPayload(
        accounts = dao.allOrdered().map { entity ->
            val secret = cipher.decrypt(entity.secret)

            try {
                BackupAccount(
                    issuer = entity.issuer,
                    label = entity.label,
                    secretBase32 = Base32.encode(secret),
                    type = entity.type.name,
                    algorithm = entity.algorithm,
                    digits = entity.digits,
                    periodSeconds = entity.periodSeconds,
                    counter = entity.counter,
                    group = entity.groupName,
                    sortOrder = entity.sortOrder,
                )
            } finally {
                secret.fill(0)
            }
        },
    )

    suspend fun importPayload(payload: BackupPayload): Int {
        var imported = 0
        var nextSortOrder = dao.allOrdered().size

        payload.accounts.forEach { account ->
            val secret = try {
                Base32.decode(account.secretBase32)
            } catch (e: Base32.InvalidBase32Exception) {
                return@forEach
            }

            val type = when (account.type.uppercase()) {
                "HOTP" -> OtpType.HOTP
                else -> OtpType.TOTP
            }

            try {
                dao.insert(
                    AccountEntity(
                        issuer = account.issuer,
                        label = account.label,
                        groupName = account.group,
                        secret = cipher.encrypt(secret),
                        type = type,
                        algorithm = account.algorithm,
                        digits = account.digits,
                        periodSeconds = account.periodSeconds,
                        counter = account.counter,
                        sortOrder = nextSortOrder++,
                    ),
                )
                imported++
            } finally {
                secret.fill(0)
            }
        }

        return imported
    }

    suspend fun move(id: Long, offset: Int) = dao.move(id, offset)

    suspend fun delete(id: Long) {
        dao.findById(id)?.let { dao.delete(it) }
    }
}
