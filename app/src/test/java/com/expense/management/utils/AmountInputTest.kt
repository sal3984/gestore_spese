package com.expense.management.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("parseAmountInput")
class AmountInputTest {

    @Test
    fun `comma decimal separator is accepted`() {
        assertEquals(12.5, parseAmountInput("12,50")!!, 0.001)
    }

    @Test
    fun `dot decimal separator is accepted`() {
        assertEquals(12.5, parseAmountInput("12.50")!!, 0.001)
    }

    @Test
    fun `integer input is accepted`() {
        assertEquals(150.0, parseAmountInput("150")!!, 0.001)
    }

    @Test
    fun `blank input is rejected`() {
        assertNull(parseAmountInput(""))
    }

    @Test
    fun `non numeric input is rejected`() {
        assertNull(parseAmountInput("abc"))
    }
}
