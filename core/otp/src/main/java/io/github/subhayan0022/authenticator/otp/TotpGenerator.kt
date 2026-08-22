package io.github.subhayan0022.authenticator.otp

/** Time-based one-time passwords, as defined by RFC 6238 (TOTP).*/
object TotpGenerator {
    const val DEFAULT_PERIOD_SECONDS = 30
    fun generate(
        secret: ByteArray,
        timeMillis: Long,
        periodSeconds: Int = DEFAULT_PERIOD_SECONDS,
        digits: Int = 6,
        algorithm: String = "HmacSHA1",
    ): String {
        require(periodSeconds > 0) { "periodSeconds must be positive, was $periodSeconds" }

        val counter = Math.floorDiv(timeMillis / 1000, periodSeconds.toLong())
        return HotpGenerator.generate(secret, counter, digits, algorithm)
    }

    /** Seconds until the current code expires*/
    fun secondsRemaining(
        timeMillis: Long,
        periodSeconds: Int = DEFAULT_PERIOD_SECONDS,
    ): Int {
        require(periodSeconds > 0) { "periodSeconds must be positive, was $periodSeconds" }

        val elapsed = Math.floorMod(timeMillis / 1000, periodSeconds.toLong()).toInt()
        return periodSeconds - elapsed
    }
}
