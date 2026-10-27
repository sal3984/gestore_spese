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
    fun `calculateEffectiveDate should return payment after closing when paymentDay before closingDay`() {
        val transactionDate = LocalDate.of(2024, 5, 20)
        val card = DateUtils.CardDateInfo(closingDay = 15, paymentDay = 10)

        val result = DateUtils.calculateEffectiveDate(transactionDate, card)

        assertEquals("2024-07-10", result)
    }

    @Test
    fun `purchase on the closing day falls into the cycle that closes`() {
        val card = DateUtils.CardDateInfo(closingDay = 20, paymentDay = 15)

        assertEquals("2024-06-15", DateUtils.calculateEffectiveDate(LocalDate.of(2024, 5, 19), card))
        assertEquals("2024-06-15", DateUtils.calculateEffectiveDate(LocalDate.of(2024, 5, 20), card))
        assertEquals("2024-07-15", DateUtils.calculateEffectiveDate(LocalDate.of(2024, 5, 21), card))
    }

    @Test
    fun `closing day beyond the month length is clamped to the last day`() {
        val card = DateUtils.CardDateInfo(closingDay = 31, paymentDay = 15)

        assertEquals("2024-03-15", DateUtils.calculateEffectiveDate(LocalDate.of(2024, 2, 28), card))
        assertEquals("2024-03-15", DateUtils.calculateEffectiveDate(LocalDate.of(2024, 2, 29), card))
        assertEquals("2024-04-15", DateUtils.calculateEffectiveDate(LocalDate.of(2024, 3, 1), card))
    }

    @Test
    fun `payment day beyond the month length is clamped to the last day`() {
        val card = DateUtils.CardDateInfo(closingDay = 10, paymentDay = 31)

        assertEquals("2024-04-30", DateUtils.calculateEffectiveDate(LocalDate.of(2024, 3, 15), card))
    }

    @Test
    fun `payment day equal to the closing day settles in the closing month`() {
        val card = DateUtils.CardDateInfo(closingDay = 15, paymentDay = 15)

        assertEquals("2024-05-15", DateUtils.calculateEffectiveDate(LocalDate.of(2024, 5, 10), card))
    }
}
