package io.github.subhayan0022.authenticator

import io.github.subhayan0022.authenticator.data.ClockCheck
import io.github.subhayan0022.authenticator.data.ClockStatus
import io.github.subhayan0022.authenticator.data.DriftLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockCheckTest {

    @Test
    fun `a clock within two seconds counts as accurate`() {
        assertEquals(DriftLevel.ACCURATE, ClockCheck.levelFor(0))
        assertEquals(DriftLevel.ACCURATE, ClockCheck.levelFor(1_999))
        assertEquals(DriftLevel.ACCURATE, ClockCheck.levelFor(-1_999))
    }

    @Test
    fun `two to fifteen seconds is minor drift`() {
        assertEquals(DriftLevel.MINOR, ClockCheck.levelFor(2_000))
        assertEquals(DriftLevel.MINOR, ClockCheck.levelFor(14_999))
    }

    @Test
    fun `fifteen seconds or more is severe`() {
        assertEquals(DriftLevel.SEVERE, ClockCheck.levelFor(15_000))
        assertEquals(DriftLevel.SEVERE, ClockCheck.levelFor(90_000))
    }

    @Test
    fun `drift is classified the same in both directions`() {
        assertEquals(ClockCheck.levelFor(20_000), ClockCheck.levelFor(-20_000))
        assertEquals(ClockCheck.levelFor(5_000), ClockCheck.levelFor(-5_000))
    }

    @Test
    fun `offset is the device clock minus network time`() {
        val ahead = ClockStatus.Measured(deviceMillis = 1_000_500, networkMillis = 1_000_000)
        val behind = ClockStatus.Measured(deviceMillis = 1_000_000, networkMillis = 1_000_500)

        assertEquals(500, ahead.offsetMillis)
        assertEquals(-500, behind.offsetMillis)
    }
}
