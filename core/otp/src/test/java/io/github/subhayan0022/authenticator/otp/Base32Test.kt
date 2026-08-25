package io.github.subhayan0022.authenticator.otp

import org.junit.Assert.assertEquals
import org.junit.Test

class Base32Test {

    private fun decodeToString(input: String) = String(Base32.decode(input))

    @Test
    fun `matches the RFC 4648 test vectors`() {
        assertEquals("f", decodeToString("MY======"))
        assertEquals("fo", decodeToString("MZXQ===="))
        assertEquals("foo", decodeToString("MZXW6==="))
        assertEquals("foob", decodeToString("MZXW6YQ="))
        assertEquals("fooba", decodeToString("MZXW6YTB"))
        assertEquals("foobar", decodeToString("MZXW6YTBOI======"))
    }

    @Test
    fun `accepts the messy input real users paste`() {
        val expected = "foobar"
        assertEquals(expected, decodeToString("mzxw6ytboi"))          // lowercase
        assertEquals(expected, decodeToString("MZXW 6YTB OI"))        // spaces
        assertEquals(expected, decodeToString("mzxw-6ytb-oi"))        // hyphens
        assertEquals(expected, decodeToString("MZXW6YTBOI"))          // no padding
    }

    @Test
    fun `decodes a real TOTP secret that then generates the right code`() {
        val secret = Base32.decode("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")

        assertEquals("12345678901234567890", String(secret))
        assertEquals("755224", HotpGenerator.generate(secret, counter = 0L))
    }

    @Test(expected = Base32.InvalidBase32Exception::class)
    fun `rejects digits that are not in the alphabet`() {
        Base32.decode("MZXW6YT1") // '1' is excluded to avoid confusion with 'I'
    }

    @Test(expected = Base32.InvalidBase32Exception::class)
    fun `rejects an empty secret`() {
        Base32.decode("   ")
    }
}
