package com.expense.management.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("Ri-blocco dopo il background")
class AppLockTimerTest {

    private var clock = 1_000_000L
    private val timer = AppLockTimer(timeoutMillis = 60_000, now = { clock })

    @ParameterizedTest(name = "fuori {0} ms -> blocca {1}")
    @CsvSource("0, false", "59999, false", "60000, true", "3600000, true")
    fun `locks only after the timeout away`(awayMillis: Long, expected: Boolean) {
        timer.onLeave()
        clock += awayMillis

        assertEquals(expected, timer.shouldLockOnReturn())
    }

    @Test
    fun `without leaving there is nothing to lock`() {
        assertFalse(timer.shouldLockOnReturn())
    }

    @Test
    fun `each departure counts for one return only`() {
        timer.onLeave()
        clock += 120_000
        timer.shouldLockOnReturn()

        clock += 120_000
        assertFalse(timer.shouldLockOnReturn())
    }

    @Test
    fun `the default timeout is one minute`() {
        assertEquals(60_000L, AppLockTimer.DEFAULT_TIMEOUT_MILLIS)
    }
}
