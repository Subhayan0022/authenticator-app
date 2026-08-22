package io.github.subhayan0022.authenticator.otp

import org.junit.Assert.assertEquals
import org.junit.Test

class TotpGeneratorTest {
    private val sha1Seed = "12345678901234567890".toByteArray()
    private val sha256Seed = "12345678901234567890123456789012".toByteArray()
    private val sha512Seed =
        "1234567890123456789012345678901234567890123456789012345678901234".toByteArray()

    /** RFC 6238 Appendix B: time in seconds paired with the expected 8-digit code. */
    private val sha1Vectors = listOf(
        59L to "94287082",
        1111111109L to "07081804",
        1111111111L to "14050471",
        1234567890L to "89005924",
        2000000000L to "69279037",
        20000000000L to "65353130",
    )

    private val sha256Vectors = listOf(
        59L to "46119246",
        1111111109L to "68084774",
        1111111111L to "67062674",
        1234567890L to "91819424",
        2000000000L to "90698825",
        20000000000L to "77737706",
    )

    private val sha512Vectors = listOf(
        59L to "90693936",
        1111111109L to "25091201",
        1111111111L to "99943326",
        1234567890L to "93441116",
        2000000000L to "38618901",
        20000000000L to "47863826",
    )

    @Test
    fun `matches the RFC 6238 SHA-1 vectors`() {
        assertVectors(sha1Seed, "HmacSHA1", sha1Vectors)
    }

    @Test
    fun `matches the RFC 6238 SHA-256 vectors`() {
        assertVectors(sha256Seed, "HmacSHA256", sha256Vectors)
    }

    @Test
    fun `matches the RFC 6238 SHA-512 vectors`() {
        assertVectors(sha512Seed, "HmacSHA512", sha512Vectors)
    }

    @Test
    fun `counts down within the current window`() {
        // 15 seconds into a 30-second window leaves 15.
        assertEquals(15, TotpGenerator.secondsRemaining(timeMillis = 15_000L))
        // At the very start of a window the full period remains.
        assertEquals(30, TotpGenerator.secondsRemaining(timeMillis = 30_000L))
        // One second before the window ends.
        assertEquals(1, TotpGenerator.secondsRemaining(timeMillis = 59_000L))
    }

    @Test
    fun `code is stable across a window and changes at the boundary`() {
        val atStart = TotpGenerator.generate(sha1Seed, timeMillis = 30_000L)
        val nearEnd = TotpGenerator.generate(sha1Seed, timeMillis = 59_999L)
        val nextWindow = TotpGenerator.generate(sha1Seed, timeMillis = 60_000L)

        assertEquals(atStart, nearEnd)
        assertEquals(false, atStart == nextWindow)
    }

    private fun assertVectors(
        seed: ByteArray,
        algorithm: String,
        vectors: List<Pair<Long, String>>,
    ) {
        vectors.forEach { (seconds, expected) ->
            assertEquals(
                "wrong code at t=$seconds for $algorithm",
                expected,
                TotpGenerator.generate(
                    secret = seed,
                    timeMillis = seconds * 1000L,
                    digits = 8,
                    algorithm = algorithm,
                ),
            )
        }
    }
}
