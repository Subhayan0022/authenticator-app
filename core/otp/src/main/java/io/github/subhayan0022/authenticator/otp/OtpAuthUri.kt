package io.github.subhayan0022.authenticator.otp

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder

enum class OtpAuthType { TOTP, HOTP }

class OtpAuthData(
    val type: OtpAuthType,
    val issuer: String,
    val label: String,
    val secret: ByteArray,
    val secretBase32: String,
    val algorithm: String,
    val digits: Int,
    val periodSeconds: Int,
    val counter: Long,
)

object OtpAuthUri {

    class InvalidUriException(message: String) : IllegalArgumentException(message)

    private const val MAX_URI_LENGTH = 2048
    private const val MAX_TEXT_LENGTH = 256

    fun parse(raw: String): OtpAuthData {
        val trimmed = raw.trim()

        if (trimmed.isEmpty()) throw InvalidUriException("Empty QR code")
        if (trimmed.length > MAX_URI_LENGTH) throw InvalidUriException("Link is too long")

        val uri = try {
            URI(trimmed.replace(" ", "%20"))
        } catch (e: URISyntaxException) {
            throw InvalidUriException("Not a valid otpauth link")
        }

        if (!"otpauth".equals(uri.scheme, ignoreCase = true)) {
            throw InvalidUriException("Not an otpauth link")
        }

        val type = when (uri.host?.lowercase()) {
            "totp" -> OtpAuthType.TOTP
            "hotp" -> OtpAuthType.HOTP
            else -> throw InvalidUriException("Unsupported type: ${uri.host ?: "none"}")
        }

        val params = queryParams(uri.rawQuery)

        val secretBase32 = params["secret"]
            ?.takeIf { it.isNotBlank() }
            ?: throw InvalidUriException("Link has no secret")
        val secret = decodeSecret(secretBase32)

        val (pathIssuer, label) = splitLabel(uri.rawPath)

        return OtpAuthData(
            type = type,
            issuer = (params["issuer"]?.takeIf { it.isNotBlank() } ?: pathIssuer)
                .take(MAX_TEXT_LENGTH),
            label = label.take(MAX_TEXT_LENGTH),
            secret = secret,
            secretBase32 = secretBase32,
            algorithm = algorithm(params["algorithm"]),
            digits = digits(params["digits"]),
            periodSeconds = period(params["period"]),
            counter = counter(params["counter"], type),
        )
    }

    private fun queryParams(rawQuery: String?): Map<String, String> =
        rawQuery.orEmpty()
            .split('&')
            .filter { it.isNotEmpty() }
            .mapNotNull { part ->
                val index = part.indexOf('=')
                if (index <= 0) return@mapNotNull null
                decode(part.substring(0, index)).lowercase() to decode(part.substring(index + 1))
            }
            .toMap()

    private fun decode(value: String): String =
        try {
            URLDecoder.decode(value, "UTF-8")
        } catch (e: IllegalArgumentException) {
            throw InvalidUriException("Link contains invalid escape characters")
        }

    private fun splitLabel(rawPath: String?): Pair<String, String> {
        val path = decode(rawPath.orEmpty().removePrefix("/")).trim()
        if (path.isEmpty()) return "" to ""

        val index = path.indexOf(':')
        return if (index < 0) {
            "" to path
        } else {
            path.substring(0, index).trim() to path.substring(index + 1).trim()
        }
    }

    private fun decodeSecret(value: String): ByteArray =
        try {
            Base32.decode(value)
        } catch (e: Base32.InvalidBase32Exception) {
            throw InvalidUriException("Secret is not valid Base32: ${e.message}")
        }

    private fun algorithm(value: String?): String = when (value?.uppercase()) {
        null, "", "SHA1" -> "HmacSHA1"
        "SHA256" -> "HmacSHA256"
        "SHA512" -> "HmacSHA512"
        else -> throw InvalidUriException("Unsupported algorithm: $value")
    }

    private fun digits(value: String?): Int {
        if (value.isNullOrEmpty()) return 6

        val digits = value.toIntOrNull() ?: throw InvalidUriException("digits is not a number")
        if (digits !in 6..8) throw InvalidUriException("digits must be 6-8, was $digits")
        return digits
    }

    private fun period(value: String?): Int {
        if (value.isNullOrEmpty()) return TotpGenerator.DEFAULT_PERIOD_SECONDS

        val period = value.toIntOrNull() ?: throw InvalidUriException("period is not a number")
        if (period !in 1..3600) throw InvalidUriException("period must be 1-3600, was $period")
        return period
    }

    private fun counter(value: String?, type: OtpAuthType): Long {
        if (type == OtpAuthType.TOTP) return 0

        val raw = value?.takeIf { it.isNotEmpty() }
            ?: throw InvalidUriException("HOTP link has no counter")

        val counter = raw.toLongOrNull() ?: throw InvalidUriException("counter is not a number")
        if (counter < 0) throw InvalidUriException("counter cannot be negative")
        return counter
    }
}
