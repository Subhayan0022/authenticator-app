package io.github.subhayan0022.authenticator.backup

import java.security.SecureRandom
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {

    private val password = "correct horse battery staple".toCharArray()
    private val plaintext = """{"version":1,"accounts":[]}""".toByteArray()

    private fun fixedRandom() = SecureRandom.getInstance("SHA1PRNG").apply { setSeed(42L) }

    private fun invalid(file: ByteArray): String =
        assertThrows(BackupCrypto.InvalidBackupException::class.java) {
            BackupCrypto.decrypt(file, password)
        }.message.orEmpty()

    @Test
    fun `round trips a payload`() {
        val file = BackupCrypto.encrypt(plaintext, password)

        assertArrayEquals(plaintext, BackupCrypto.decrypt(file, password))
    }

    @Test
    fun `file starts with the magic header`() {
        val file = BackupCrypto.encrypt(plaintext, password)

        assertEquals("TOTPBAK1", String(file.copyOfRange(0, 8), Charsets.US_ASCII))
    }

    @Test
    fun `plaintext never appears in the file`() {
        val secret = """{"secretBase32":"JBSWY3DPEHPK3PXP"}""".toByteArray()
        val file = BackupCrypto.encrypt(secret, password)

        assertEquals(-1, String(file, Charsets.ISO_8859_1).indexOf("JBSWY3DPEHPK3PXP"))
    }

    @Test
    fun `same input twice produces different files`() {
        val a = BackupCrypto.encrypt(plaintext, password)
        val b = BackupCrypto.encrypt(plaintext, password)

        assertNotEquals(
            String(a, Charsets.ISO_8859_1),
            String(b, Charsets.ISO_8859_1),
        )
    }

    @Test
    fun `wrong password is rejected`() {
        val file = BackupCrypto.encrypt(plaintext, password)

        assertThrows(BackupCrypto.WrongPasswordException::class.java) {
            BackupCrypto.decrypt(file, "wrong password".toCharArray())
        }
    }

    @Test
    fun `empty password differs from the real one`() {
        val file = BackupCrypto.encrypt(plaintext, password)

        assertThrows(BackupCrypto.WrongPasswordException::class.java) {
            BackupCrypto.decrypt(file, CharArray(0))
        }
    }

    @Test
    fun `tampering with the ciphertext is detected`() {
        val file = BackupCrypto.encrypt(plaintext, password)
        file[file.size - 1] = (file[file.size - 1] + 1).toByte()

        assertThrows(BackupCrypto.WrongPasswordException::class.java) {
            BackupCrypto.decrypt(file, password)
        }
    }

    @Test
    fun `tampering with the stored memory cost is detected`() {
        val file = BackupCrypto.encrypt(plaintext, password)
        file[10] = (file[10] + 1).toByte()

        val thrown = assertThrows(IllegalArgumentException::class.java) {
            BackupCrypto.decrypt(file, password)
        }
        assertTrue(
            thrown is BackupCrypto.WrongPasswordException ||
                thrown is BackupCrypto.InvalidBackupException,
        )
    }

    @Test
    fun `tampering with the salt is detected`() {
        val file = BackupCrypto.encrypt(plaintext, password)
        file[20] = (file[20] + 1).toByte()

        assertThrows(BackupCrypto.WrongPasswordException::class.java) {
            BackupCrypto.decrypt(file, password)
        }
    }

    @Test
    fun `rejects a file that is not a backup`() {
        val notABackup = ByteArray(200) { it.toByte() }

        assertEquals("Not a backup file, or an unsupported format version", invalid(notABackup))
    }

    @Test
    fun `rejects a truncated file`() {
        val file = BackupCrypto.encrypt(plaintext, password)

        assertEquals("File is too small to be a backup", invalid(file.copyOfRange(0, 20)))
    }

    @Test
    fun `rejects an unknown key derivation function`() {
        val file = BackupCrypto.encrypt(plaintext, password)
        file[8] = 99

        assertEquals("Unsupported key derivation function: 99", invalid(file))
    }

    @Test
    fun `rejects an absurd memory cost before running the KDF`() {
        val file = BackupCrypto.encrypt(plaintext, password)
        file[9] = 0x7F
        file[10] = 0xFF.toByte()
        file[11] = 0xFF.toByte()
        file[12] = 0xFF.toByte()

        assertTrue(invalid(file).startsWith("Backup declares an unusable memory cost"))
    }

    @Test
    fun `rejects an absurd iteration count before running the KDF`() {
        val file = BackupCrypto.encrypt(plaintext, password)
        file[13] = 0x7F
        file[14] = 0xFF.toByte()
        file[15] = 0xFF.toByte()
        file[16] = 0xFF.toByte()

        assertTrue(invalid(file).startsWith("Backup declares an unusable iteration count"))
    }

    @Test
    fun `rejects a zero salt length`() {
        val file = BackupCrypto.encrypt(plaintext, password)
        file[18] = 0

        assertTrue(invalid(file).startsWith("Backup declares an unusable salt length"))
    }

    @Test
    fun `survives a large payload`() {
        val big = ByteArray(200_000) { (it % 251).toByte() }
        val file = BackupCrypto.encrypt(big, password)

        assertArrayEquals(big, BackupCrypto.decrypt(file, password))
    }

    @Test
    fun `unicode passwords round trip`() {
        val unicode = "pässwörd-日本語-🔐".toCharArray()
        val file = BackupCrypto.encrypt(plaintext, unicode, fixedRandom())

        assertArrayEquals(plaintext, BackupCrypto.decrypt(file, unicode))
    }
}
