package io.github.subhayan0022.authenticator.otp

import org.junit.Assert.assertEquals
import org.junit.Test

class HotpGeneratorTest {
    private val secret = "12345678901234567890".toByteArray()

    @Test
    fun `matches the RFC 4226 test vectors`() {
        val expected = listOf(
            "755224", "287082", "359152", "969429", "338314",
            "254676", "287922", "162583", "399871", "520489",
        )

        expected.forEachIndexed { counter, code ->
            assertEquals(
                "wrong code for counter $counter",
                code,
                HotpGenerator.generate(secret, counter.toLong()),
            )
        }
    }

    @Test
    fun `pads short codes with leading zeros`() {
        val code = HotpGenerator.generate(secret, counter = 0L, digits = 8)
        assertEquals(8, code.length)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects an unsupported digit count`() {
        HotpGenerator.generate(secret, counter = 0L, digits = 4)
    }
}
