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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val RESERVED_GROUP = "All"

fun isReservedGroup(name: String): Boolean = name.trim().equals(RESERVED_GROUP, ignoreCase = true)

class AccountRepository(
    private val dao: AccountDao,
    private val groupDao: GroupDao,
    private val cipher: SecretCipher = KeystoreSecretCipher,
) {

    private val session = mutableMapOf<Long, ByteArray>()

    val isSessionOpen: Boolean
        get() = session.isNotEmpty()

    suspend fun openSession() {
        closeSession()
        dao.allOrdered().forEach { entity ->
            session[entity.id] = cipher.decrypt(entity.secret)
        }
    }

    fun closeSession() {
        session.values.forEach { it.fill(0) }
        session.clear()
    }

    fun observeGroups(): Flow<List<String>> =
        groupDao.observeNames().map { names -> names.filterNot { isReservedGroup(it) } }

    suspend fun createGroup(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && !isReservedGroup(trimmed)) {
            groupDao.insert(GroupEntity(trimmed))
        }
    }

    suspend fun renameGroup(old: String, new: String) {
        val trimmed = new.trim()
        if (trimmed.isNotEmpty() && trimmed != old && !isReservedGroup(trimmed)) {
            groupDao.rename(old, trimmed)
        }
    }

    suspend fun deleteGroup(name: String) = groupDao.remove(name)

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
        ).also { id ->
            if (isSessionOpen) session[id] = secret.copyOf()
        }
    } finally {
        secret.fill(0)
    }

    /** Decrypts, generates, and discards. Returns null if the account is gone. */
    suspend fun currentCode(
        id: Long,
        timeMillis: Long = System.currentTimeMillis(),
    ): String? {
        val entity = dao.findById(id) ?: return null

        val fromSession = session[entity.id]
        val secret = fromSession ?: cipher.decrypt(entity.secret)

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
            if (fromSession == null) secret.fill(0)
        }
    }

    /** Consumes the next HOTP counter value. Each call yields a different code. */
    suspend fun advanceHotp(id: Long): String? {
        val counter = dao.nextCounter(id) ?: return null
        val entity = dao.findById(id) ?: return null

        val fromSession = session[entity.id]
        val secret = fromSession ?: cipher.decrypt(entity.secret)

        return try {
            HotpGenerator.generate(secret, counter, entity.digits, entity.algorithm)
        } finally {
            if (fromSession == null) secret.fill(0)
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
        groups = groupDao.observeNames().first(),
        accounts = dao.allOrdered().map { entity ->
            val fromSession = session[entity.id]
            val secret = fromSession ?: cipher.decrypt(entity.secret)

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
                if (fromSession == null) secret.fill(0)
            }
        },
    )

    suspend fun importPayload(payload: BackupPayload): Int {
        payload.groups.filterNot { isReservedGroup(it) }.forEach {
            groupDao.insert(GroupEntity(it))
        }
        payload.accounts.mapNotNull { it.group }
            .distinct()
            .filterNot { isReservedGroup(it) }
            .forEach { groupDao.insert(GroupEntity(it)) }

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
                ).also { newId ->
                    if (isSessionOpen) session[newId] = secret.copyOf()
                }
                imported++
            } finally {
                secret.fill(0)
            }
        }

        return imported
    }

    suspend fun move(id: Long, offset: Int) = dao.move(id, offset)

    suspend fun delete(id: Long) {
        session.remove(id)?.fill(0)
        dao.findById(id)?.let { dao.delete(it) }
    }
}
