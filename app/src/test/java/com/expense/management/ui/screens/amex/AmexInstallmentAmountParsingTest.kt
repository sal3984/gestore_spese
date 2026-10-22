package com.expense.management.ui.screens.amex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AmexInstallmentAmountParsingTest {

    @Test
    fun `comma decimal separator is accepted`() {
        assertEquals(12.5, parseInstallmentAmount("12,50")!!, 0.001)
    }

    @Test
    fun `dot decimal separator is accepted`() {
        assertEquals(12.5, parseInstallmentAmount("12.50")!!, 0.001)
    }

    @Test
    fun `integer input is accepted`() {
        assertEquals(150.0, parseInstallmentAmount("150")!!, 0.001)
    }

    @Test
    fun `blank input is rejected`() {
        assertNull(parseInstallmentAmount(""))
    }

    @Test
    fun `non numeric input is rejected`() {
        assertNull(parseInstallmentAmount("abc"))
    }
}
