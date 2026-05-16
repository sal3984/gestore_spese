package com.expense.management.utils

import com.expense.management.data.CreditCardEntity
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun `calculateEffectiveDate should return transaction date when card has no closing or payment day`() {
        // Given
        val transactionDate = LocalDate.of(2024, 5, 15)
        val card = mockk<CreditCardEntity> {
            every { closingDay } returns 0
            every { paymentDay } returns 0
        }

        // When
        val result = DateUtils.calculateEffectiveDate(transactionDate, card)

        // Then
        assertEquals("2024-05-15", result)
    }

    @Test
    fun `calculateEffectiveDate should return same month payment if before closing day`() {
        // Given
        val transactionDate = LocalDate.of(2024, 5, 10)
        val card = mockk<CreditCardEntity> {
            every { closingDay } returns 15
            every { paymentDay } returns 25
        }

        // When
        val result = DateUtils.calculateEffectiveDate(transactionDate, card)

        // Then
        assertEquals("2024-05-25", result)
    }

    @Test
    fun `calculateEffectiveDate should return next month payment if after closing day`() {
        // Given
        val transactionDate = LocalDate.of(2024, 5, 20)
        val card = mockk<CreditCardEntity> {
            every { closingDay } returns 15
            every { paymentDay } returns 10
        }

        // When
        val result = DateUtils.calculateEffectiveDate(transactionDate, card)

        // Then
        assertEquals("2024-06-10", result)
    }
}
