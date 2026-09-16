package io.github.subhayan0022.authenticator.backup

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFileTest {

    private val password = "correct horse battery staple".toCharArray()

    private val accounts = listOf(
        BackupAccount(
            issuer = "GitHub",
            label = "alice@example.com",
            secretBase32 = "JBSWY3DPEHPK3PXP",
            group = "Work",
            sortOrder = 0,
        ),
        BackupAccount(
            issuer = "Fastmail",
            label = "hrik@example.com",
            secretBase32 = "MZXW6YTBOI======",
            algorithm = "HmacSHA256",
            digits = 8,
            periodSeconds = 45,
            sortOrder = 1,
        ),
        BackupAccount(
            issuer = "Legacy",
            label = "counter-based",
            secretBase32 = "GEZDGNBVGY3TQOJQ",
            type = "HOTP",
            counter = 7,
            sortOrder = 2,
        ),
    )

    private fun encryptRaw(jsonText: String): ByteArray =
        BackupCrypto.encrypt(jsonText.toByteArray(Charsets.UTF_8), password)

    @Test
    fun `round trips every field`() {
        val file = BackupFile.write(BackupPayload(accounts = accounts), password)
        val restored = BackupFile.read(file, password)

        assertEquals(BackupPayload.CURRENT_VERSION, restored.version)
        assertEquals(accounts, restored.accounts)
    }

    @Test
    fun `round trips an empty account list`() {
        val file = BackupFile.write(BackupPayload(accounts = emptyList()), password)

        assertEquals(emptyList<BackupAccount>(), BackupFile.read(file, password).accounts)
    }

    @Test
    fun `secrets do not survive in the encrypted file`() {
        val file = BackupFile.write(BackupPayload(accounts = accounts), password)
        val asText = String(file, Charsets.ISO_8859_1)

        assertEquals(-1, asText.indexOf("JBSWY3DPEHPK3PXP"))
        assertEquals(-1, asText.indexOf("GitHub"))
    }

    @Test
    fun `wrong password cannot read the payload`() {
        val file = BackupFile.write(BackupPayload(accounts = accounts), password)

        assertThrows(BackupCrypto.WrongPasswordException::class.java) {
            BackupFile.read(file, "nope".toCharArray())
        }
    }

    @Test
    fun `a field added by a future version is ignored`() {
        val future = """
            {"version":1,"accounts":[
              {"issuer":"GitHub","label":"alice","secretBase32":"JBSWY3DPEHPK3PXP",
               "notes":"added in a later release","icon":"github.png"}
            ]}
        """.trimIndent()

        val restored = BackupFile.read(encryptRaw(future), password)

        assertEquals(1, restored.accounts.size)
        assertEquals("GitHub", restored.accounts[0].issuer)
        assertEquals("JBSWY3DPEHPK3PXP", restored.accounts[0].secretBase32)
    }

    @Test
    fun `a field missing from an older version falls back to its default`() {
        val old = """{"version":1,"accounts":[
            {"issuer":"GitHub","label":"alice","secretBase32":"JBSWY3DPEHPK3PXP"}
        ]}"""

        val account = BackupFile.read(encryptRaw(old), password).accounts.single()

        assertEquals("TOTP", account.type)
        assertEquals("HmacSHA1", account.algorithm)
        assertEquals(6, account.digits)
        assertEquals(30, account.periodSeconds)
        assertEquals(0L, account.counter)
        assertNull(account.group)
        assertEquals(0, account.sortOrder)
    }

    @Test
    fun `a newer payload version is refused rather than guessed at`() {
        val newer = """{"version":99,"accounts":[]}"""

        val message = assertThrows(BackupCrypto.InvalidBackupException::class.java) {
            BackupFile.read(encryptRaw(newer), password)
        }.message.orEmpty()

        assertTrue(message.startsWith("This backup was made by a newer version"))
    }

    @Test
    fun `valid encryption wrapping invalid json reports a content error`() {
        val message = assertThrows(BackupCrypto.InvalidBackupException::class.java) {
            BackupFile.read(encryptRaw("this is not json"), password)
        }.message.orEmpty()

        assertEquals("Backup contents could not be read", message)
    }

    @Test
    fun `defaults are written explicitly so the file is self describing`() {
        val file = BackupFile.write(
            BackupPayload(accounts = listOf(accounts[0])),
            password,
        )
        val decrypted = String(BackupCrypto.decrypt(file, password), Charsets.UTF_8)
        val parsed = Json.parseToJsonElement(decrypted)

        assertTrue(parsed.toString().contains("\"digits\":6"))
        assertTrue(parsed.toString().contains("\"periodSeconds\":30"))
    }
}
