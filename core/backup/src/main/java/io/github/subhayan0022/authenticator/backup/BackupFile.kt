package io.github.subhayan0022.authenticator.backup

import java.security.SecureRandom
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object BackupFile {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun write(
        payload: BackupPayload,
        password: CharArray,
        random: SecureRandom = SecureRandom(),
    ): ByteArray {
        val bytes = json
            .encodeToString(BackupPayload.serializer(), payload)
            .toByteArray(Charsets.UTF_8)

        return try {
            BackupCrypto.encrypt(bytes, password, random)
        } finally {
            bytes.fill(0)
        }
    }

    fun read(file: ByteArray, password: CharArray): BackupPayload {
        val bytes = BackupCrypto.decrypt(file, password)

        return try {
            val payload = json.decodeFromString(
                BackupPayload.serializer(),
                String(bytes, Charsets.UTF_8),
            )

            if (payload.version > BackupPayload.CURRENT_VERSION) {
                throw BackupCrypto.InvalidBackupException(
                    "This backup was made by a newer version of the app " +
                        "(format ${payload.version}, this app reads ${BackupPayload.CURRENT_VERSION})",
                )
            }

            payload
        } catch (e: SerializationException) {
            throw BackupCrypto.InvalidBackupException("Backup contents could not be read")
        } finally {
            bytes.fill(0)
        }
    }
}
