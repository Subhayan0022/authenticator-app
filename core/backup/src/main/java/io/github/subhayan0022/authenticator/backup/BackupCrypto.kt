package io.github.subhayan0022.authenticator.backup

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

object BackupCrypto {

    class InvalidBackupException(message: String) : IllegalArgumentException(message)

    class WrongPasswordException : IllegalArgumentException(
        "Wrong password, or the file has been altered",
    )

    private val MAGIC = "TOTPBAK1".toByteArray(Charsets.US_ASCII)

    private const val KDF_ARGON2ID = 1
    private const val SALT_LENGTH = 16
    private const val NONCE_LENGTH = 12
    private const val KEY_LENGTH = 32
    private const val GCM_TAG_BITS = 128
    private const val GCM_TAG_BYTES = 16
    private const val PARAM_BYTES = 11

    const val DEFAULT_MEMORY_KIB = 65_536
    const val DEFAULT_ITERATIONS = 3
    const val DEFAULT_PARALLELISM = 4

    private const val MIN_MEMORY_KIB = 8
    private const val MAX_MEMORY_KIB = 262_144
    private const val MAX_ITERATIONS = 16
    private const val MAX_PARALLELISM = 16
    private const val MIN_SALT_LENGTH = 8
    private const val MAX_SALT_LENGTH = 64

    fun encrypt(
        plaintext: ByteArray,
        password: CharArray,
        random: SecureRandom = SecureRandom(),
    ): ByteArray {
        val salt = ByteArray(SALT_LENGTH).also(random::nextBytes)
        val nonce = ByteArray(NONCE_LENGTH).also(random::nextBytes)

        val header = buildHeader(
            memoryKiB = DEFAULT_MEMORY_KIB,
            iterations = DEFAULT_ITERATIONS,
            parallelism = DEFAULT_PARALLELISM,
            salt = salt,
            nonce = nonce,
        )

        val key = deriveKey(
            password = password,
            salt = salt,
            memoryKiB = DEFAULT_MEMORY_KIB,
            iterations = DEFAULT_ITERATIONS,
            parallelism = DEFAULT_PARALLELISM,
        )

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(GCM_TAG_BITS, nonce),
            )
            cipher.updateAAD(header)
            header + cipher.doFinal(plaintext)
        } finally {
            key.fill(0)
        }
    }

    fun decrypt(file: ByteArray, password: CharArray): ByteArray {
        if (file.size < MAGIC.size + PARAM_BYTES + MIN_SALT_LENGTH + NONCE_LENGTH + GCM_TAG_BYTES) {
            throw InvalidBackupException("File is too small to be a backup")
        }

        if (!file.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw InvalidBackupException("Not a backup file, or an unsupported format version")
        }

        val buffer = ByteBuffer.wrap(file).order(ByteOrder.BIG_ENDIAN)
        buffer.position(MAGIC.size)

        val kdfId = buffer.get().toInt() and 0xFF
        if (kdfId != KDF_ARGON2ID) {
            throw InvalidBackupException("Unsupported key derivation function: $kdfId")
        }

        val memoryKiB = buffer.int
        val iterations = buffer.int
        val parallelism = buffer.get().toInt() and 0xFF
        val saltLength = buffer.get().toInt() and 0xFF

        if (memoryKiB !in MIN_MEMORY_KIB..MAX_MEMORY_KIB) {
            throw InvalidBackupException("Backup declares an unusable memory cost: $memoryKiB KiB")
        }
        if (iterations !in 1..MAX_ITERATIONS) {
            throw InvalidBackupException("Backup declares an unusable iteration count: $iterations")
        }
        if (parallelism !in 1..MAX_PARALLELISM) {
            throw InvalidBackupException("Backup declares an unusable parallelism: $parallelism")
        }
        if (saltLength !in MIN_SALT_LENGTH..MAX_SALT_LENGTH) {
            throw InvalidBackupException("Backup declares an unusable salt length: $saltLength")
        }
        if (buffer.remaining() < saltLength + NONCE_LENGTH + GCM_TAG_BYTES) {
            throw InvalidBackupException("Backup file is truncated")
        }

        val salt = ByteArray(saltLength).also(buffer::get)
        val nonce = ByteArray(NONCE_LENGTH).also(buffer::get)

        val headerLength = buffer.position()
        val header = file.copyOfRange(0, headerLength)
        val ciphertext = file.copyOfRange(headerLength, file.size)

        val key = deriveKey(password, salt, memoryKiB, iterations, parallelism)

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(GCM_TAG_BITS, nonce),
            )
            cipher.updateAAD(header)
            cipher.doFinal(ciphertext)
        } catch (e: AEADBadTagException) {
            throw WrongPasswordException()
        } finally {
            key.fill(0)
        }
    }

    private fun buildHeader(
        memoryKiB: Int,
        iterations: Int,
        parallelism: Int,
        salt: ByteArray,
        nonce: ByteArray,
    ): ByteArray = ByteBuffer
        .allocate(MAGIC.size + PARAM_BYTES + salt.size + nonce.size)
        .order(ByteOrder.BIG_ENDIAN)
        .put(MAGIC)
        .put(KDF_ARGON2ID.toByte())
        .putInt(memoryKiB)
        .putInt(iterations)
        .put(parallelism.toByte())
        .put(salt.size.toByte())
        .put(salt)
        .put(nonce)
        .array()

    private fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        memoryKiB: Int,
        iterations: Int,
        parallelism: Int,
    ): ByteArray {
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withSalt(salt)
            .withMemoryAsKB(memoryKiB)
            .withIterations(iterations)
            .withParallelism(parallelism)
            .build()

        val generator = Argon2BytesGenerator().apply { init(parameters) }
        val key = ByteArray(KEY_LENGTH)
        generator.generateBytes(password, key)
        return key
    }
}
