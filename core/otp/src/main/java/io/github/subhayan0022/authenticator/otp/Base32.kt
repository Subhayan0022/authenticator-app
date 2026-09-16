package io.github.subhayan0022.authenticator.otp
object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    class InvalidBase32Exception(message: String) : IllegalArgumentException(message)
    fun decode(input: String): ByteArray {
        val cleaned = input
            .uppercase()
            .filterNot { it.isWhitespace() || it == '-' || it == '=' }

        if (cleaned.isEmpty()) throw InvalidBase32Exception("secret is empty")

        val output = ByteArray(cleaned.length * 5 / 8)
        var outputIndex = 0

        var buffer = 0
        var bitsInBuffer = 0

        for (char in cleaned) {
            val value = ALPHABET.indexOf(char)
            if (value < 0) throw InvalidBase32Exception("'$char' is not a Base32 character")

            buffer = (buffer shl 5) or value
            bitsInBuffer += 5

            if (bitsInBuffer >= 8) {
                bitsInBuffer -= 8
                output[outputIndex++] = (buffer ushr bitsInBuffer).toByte()
            }
        }

        if (bitsInBuffer > 0 && (buffer and ((1 shl bitsInBuffer) - 1)) != 0) {
            throw InvalidBase32Exception("secret ends mid-byte and cannot be decoded")
        }

        return output
    }

    fun encode(data: ByteArray): String {
        if (data.isEmpty()) return ""

        val out = StringBuilder()
        var buffer = 0
        var bits = 0

        for (byte in data) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bits += 8

            while (bits >= 5) {
                out.append(ALPHABET[(buffer ushr (bits - 5)) and 0x1F])
                bits -= 5
            }
        }

        if (bits > 0) {
            out.append(ALPHABET[(buffer shl (5 - bits)) and 0x1F])
        }

        while (out.length % 8 != 0) {
            out.append('=')
        }

        return out.toString()
    }
}
