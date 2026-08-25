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
}
