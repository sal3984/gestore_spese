package com.expense.management.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun `calculateEffectiveDate should return transaction date when card has no closing or payment day`() {
        // Given
        val transactionDate = LocalDate.of(2024, 5, 15)
        val card = DateUtils.CardDateInfo(closingDay = 0, paymentDay = 0)

        // When
        val result = DateUtils.calculateEffectiveDate(transactionDate, card)

        // Then
        assertEquals("2024-05-15", result)
    }

    @Test
    fun `calculateEffectiveDate should return same month payment if before closing day`() {
        // Given
        val transactionDate = LocalDate.of(2024, 5, 10)
        val card = DateUtils.CardDateInfo(closingDay = 15, paymentDay = 25)

        // When
        val result = DateUtils.calculateEffectiveDate(transactionDate, card)

        // Then
        assertEquals("2024-05-25", result)
    }

    @Test
    fun `calculateEffectiveDate should return next month payment if after closing day`() {
        // Given
        val transactionDate = LocalDate.of(2024, 5, 20)
        val card = DateUtils.CardDateInfo(closingDay = 15, paymentDay = 10)

        // When
        val result = DateUtils.calculateEffectiveDate(transactionDate, card)

        // Then
        assertEquals("2024-06-10", result)
    }
}
