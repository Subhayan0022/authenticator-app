package io.github.subhayan0022.authenticator.otp

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HotpGenerator {
    fun generate(
        secret: ByteArray,
        counter: Long,
        digits: Int = 6,
        algorithm: String = "HmacSHA1",
    ): String {
        require(digits in 6..8) { "digits must be between 6 and 8, was $digits" }

        // Turn the counter into 8 bytes.
        val counterBytes = ByteArray(8)
        var remaining = counter
        for (i in 7 downTo 0) {
            counterBytes[i] = (remaining and 0xFF).toByte()
            remaining = remaining ushr 8
        }

        // HMAC the counter bytes with the secret.
        val mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(secret, algorithm))
        val hash = mac.doFinal(counterBytes)

        // Pick 4 bytes and read them as one number.
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary =
            ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        // Keep the last `digits` digits, padded with leading zeros.
        var modulus = 1
        repeat(digits) { modulus *= 10 }
        return (binary % modulus).toString().padStart(digits, '0')
    }
}
