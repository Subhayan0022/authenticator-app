package io.github.subhayan0022.authenticator.otp

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OtpAuthUriTest {

    private fun invalid(uri: String): String =
        assertThrows(OtpAuthUri.InvalidUriException::class.java) { OtpAuthUri.parse(uri) }
            .message.orEmpty()

    // ---------- well-formed links ----------

    @Test
    fun `parses a minimal totp link`() {
        val data = OtpAuthUri.parse("otpauth://totp/alice@example.com?secret=JBSWY3DPEHPK3PXP")

        assertEquals(OtpAuthType.TOTP, data.type)
        assertEquals("", data.issuer)
        assertEquals("alice@example.com", data.label)
        assertEquals("HmacSHA1", data.algorithm)
        assertEquals(6, data.digits)
        assertEquals(30, data.periodSeconds)
        assertEquals(0L, data.counter)
    }

    @Test
    fun `parses issuer from the label prefix`() {
        val data = OtpAuthUri.parse("otpauth://totp/GitHub:alice?secret=JBSWY3DPEHPK3PXP")

        assertEquals("GitHub", data.issuer)
        assertEquals("alice", data.label)
    }

    @Test
    fun `issuer parameter wins over the label prefix`() {
        val data = OtpAuthUri.parse(
            "otpauth://totp/Legacy:alice?secret=JBSWY3DPEHPK3PXP&issuer=GitHub",
        )

        assertEquals("GitHub", data.issuer)
        assertEquals("alice", data.label)
    }

    @Test
    fun `decodes percent-encoded issuer and label`() {
        val data = OtpAuthUri.parse(
            "otpauth://totp/ACME%20Co:john%40example.com?secret=JBSWY3DPEHPK3PXP",
        )

        assertEquals("ACME Co", data.issuer)
        assertEquals("john@example.com", data.label)
    }

    @Test
    fun `tolerates unencoded spaces that java net URI would reject`() {
        val data = OtpAuthUri.parse("otpauth://totp/ACME Co:alice?secret=JBSWY3DPEHPK3PXP")

        assertEquals("ACME Co", data.issuer)
        assertEquals("alice", data.label)
    }

    @Test
    fun `honours all optional parameters`() {
        val data = OtpAuthUri.parse(
            "otpauth://totp/x?secret=JBSWY3DPEHPK3PXP&algorithm=SHA256&digits=8&period=60",
        )

        assertEquals("HmacSHA256", data.algorithm)
        assertEquals(8, data.digits)
        assertEquals(60, data.periodSeconds)
    }

    @Test
    fun `parameter names and scheme are case insensitive`() {
        val data = OtpAuthUri.parse("OTPAUTH://TOTP/x?SECRET=JBSWY3DPEHPK3PXP&ALGORITHM=sha512")

        assertEquals(OtpAuthType.TOTP, data.type)
        assertEquals("HmacSHA512", data.algorithm)
    }

    @Test
    fun `parses an hotp link with a counter`() {
        val data = OtpAuthUri.parse("otpauth://hotp/x?secret=JBSWY3DPEHPK3PXP&counter=7")

        assertEquals(OtpAuthType.HOTP, data.type)
        assertEquals(7L, data.counter)
    }

    @Test
    fun `secret decodes to the same bytes as the Base32 decoder`() {
        val data = OtpAuthUri.parse("otpauth://totp/x?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")

        assertArrayEquals("12345678901234567890".toByteArray(), data.secret)
    }

    @Test
    fun `parsed link generates the RFC 4226 first code`() {
        val data = OtpAuthUri.parse("otpauth://totp/x?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")

        assertEquals("755224", HotpGenerator.generate(data.secret, 0, data.digits, data.algorithm))
    }

    // ---------- malformed and hostile links ----------

    @Test
    fun `rejects an empty string`() {
        assertEquals("Empty QR code", invalid(""))
    }

    @Test
    fun `rejects a non-otpauth scheme`() {
        assertEquals("Not an otpauth link", invalid("https://example.com/?secret=JBSWY3DPEHPK3PXP"))
    }

    @Test
    fun `rejects the migration scheme that Phase 6 will handle`() {
        assertEquals("Not an otpauth link", invalid("otpauth-migration://offline?data=AAAA"))
    }

    @Test
    fun `rejects an unsupported otp type`() {
        assertEquals("Unsupported type: steam", invalid("otpauth://steam/x?secret=JBSWY3DPEHPK3PXP"))
    }

    @Test
    fun `rejects a link with no secret`() {
        assertEquals("Link has no secret", invalid("otpauth://totp/alice"))
    }

    @Test
    fun `rejects a secret that is not Base32`() {
        assert(invalid("otpauth://totp/x?secret=NOT_BASE32!").startsWith("Secret is not valid Base32"))
    }

    @Test
    fun `rejects an unsupported algorithm`() {
        assertEquals(
            "Unsupported algorithm: MD5",
            invalid("otpauth://totp/x?secret=JBSWY3DPEHPK3PXP&algorithm=MD5"),
        )
    }

    @Test
    fun `rejects digits outside the range HotpGenerator accepts`() {
        assertEquals(
            "digits must be 6-8, was 10",
            invalid("otpauth://totp/x?secret=JBSWY3DPEHPK3PXP&digits=10"),
        )
        assertEquals(
            "digits must be 6-8, was 0",
            invalid("otpauth://totp/x?secret=JBSWY3DPEHPK3PXP&digits=0"),
        )
    }

    @Test
    fun `rejects a non-numeric digits value`() {
        assertEquals(
            "digits is not a number",
            invalid("otpauth://totp/x?secret=JBSWY3DPEHPK3PXP&digits=six"),
        )
    }

    @Test
    fun `rejects a zero or negative period`() {
        assertEquals(
            "period must be 1-3600, was 0",
            invalid("otpauth://totp/x?secret=JBSWY3DPEHPK3PXP&period=0"),
        )
        assertEquals(
            "period must be 1-3600, was -30",
            invalid("otpauth://totp/x?secret=JBSWY3DPEHPK3PXP&period=-30"),
        )
    }

    @Test
    fun `rejects an hotp link with no counter`() {
        assertEquals("HOTP link has no counter", invalid("otpauth://hotp/x?secret=JBSWY3DPEHPK3PXP"))
    }

    @Test
    fun `rejects a negative hotp counter`() {
        assertEquals(
            "counter cannot be negative",
            invalid("otpauth://hotp/x?secret=JBSWY3DPEHPK3PXP&counter=-1"),
        )
    }

    @Test
    fun `rejects an oversized link`() {
        val huge = "otpauth://totp/x?secret=JBSWY3DPEHPK3PXP&issuer=" + "A".repeat(3000)

        assertEquals("Link is too long", invalid(huge))
    }

    @Test
    fun `truncates an issuer long enough to wreck the list UI`() {
        val data = OtpAuthUri.parse(
            "otpauth://totp/x?secret=JBSWY3DPEHPK3PXP&issuer=" + "A".repeat(1000),
        )

        assertEquals(256, data.issuer.length)
    }

    @Test
    fun `does not double decode an escaped ampersand into a second parameter`() {
        val data = OtpAuthUri.parse(
            "otpauth://totp/x?secret=JBSWY3DPEHPK3PXP&issuer=A%26digits%3D8",
        )

        assertEquals("A&digits=8", data.issuer)
        assertEquals(6, data.digits)
    }
}
