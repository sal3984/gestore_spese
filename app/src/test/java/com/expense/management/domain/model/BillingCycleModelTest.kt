package com.expense.management.domain.model

import com.expense.management.utils.DateUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import java.time.YearMonth

@DisplayName("Modello del ciclo di fatturazione")
class BillingCycleModelTest {

    @Nested
    @DisplayName("CardCycleRules")
    inner class Rules {

        @ParameterizedTest(name = "chiusura {0}, addebito {1} -> offset {2}")
        @CsvSource("20, 15, 1", "10, 25, 0", "15, 15, 0", "31, 1, 1", "1, 31, 0")
        fun `fromCardDays derives the due month like the card setup`(closingDay: Int, paymentDay: Int, offset: Int) {
            assertEquals(CardCycleRules(closingDay, paymentDay, offset), CardCycleRules.fromCardDays(closingDay, paymentDay))
        }

        @ParameterizedTest(name = "chiusura {0}, addebito {1} -> nessun ciclo")
        @CsvSource("0, 15", "15, 0", "32, 5", "5, 32", "-1, 10")
        fun `fromCardDays returns null for a card without a cycle`(closingDay: Int, paymentDay: Int) {
            assertNull(CardCycleRules.fromCardDays(closingDay, paymentDay))
        }

        @Test
        fun `fromCardDays agrees with DateUtils on every pair of days`() {
            // Acquisto il 1 gennaio, prima di ogni chiusura dal 2 al 31: il ciclo e' gennaio
            val purchase = LocalDate.of(2025, 1, 1)
            for (closingDay in 2..31) {
                for (paymentDay in 2..31) {
                    val rules = requireNotNull(CardCycleRules.fromCardDays(closingDay, paymentDay))
                    val dueMonth = YearMonth.from(purchase).plusMonths(rules.dueMonthOffset.toLong())
                    val expected = dueMonth.atDay(minOf(paymentDay, dueMonth.lengthOfMonth())).toString()

                    val actual = DateUtils.calculateEffectiveDate(purchase, DateUtils.CardDateInfo(closingDay, paymentDay))

                    assertEquals(expected, actual, "chiusura $closingDay, addebito $paymentDay")
                }
            }
        }

        @Test
        fun `rejects days and offsets out of range`() {
            assertThrows<IllegalArgumentException> { CardCycleRules(0, 15, 1) }
            assertThrows<IllegalArgumentException> { CardCycleRules(20, 32, 1) }
            assertThrows<IllegalArgumentException> { CardCycleRules(20, 15, 3) }
            assertThrows<IllegalArgumentException> { CardCycleRules(20, 15, -1) }
        }

        @Test
        fun `rejects a due day before closing in the same month`() {
            assertThrows<IllegalArgumentException> { CardCycleRules(closingDay = 20, dueDay = 15, dueMonthOffset = 0) }
        }
    }

    @Nested
    @DisplayName("CycleStatus")
    inner class Status {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource("OPEN, CLOSED", "CLOSED, PAID", "CLOSED, PARTIALLY_PAID", "PARTIALLY_PAID, PAID", "PAID, CLOSED", "PARTIALLY_PAID, CLOSED")
        fun `allows the cycle lifecycle and the payment revert`(from: CycleStatus, to: CycleStatus) {
            assertTrue(from.canMoveTo(to))
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource("OPEN, PAID", "OPEN, PARTIALLY_PAID", "CLOSED, OPEN", "PAID, OPEN", "PAID, PARTIALLY_PAID", "OPEN, OPEN", "PAID, PAID")
        fun `rejects skipping the closing, reopening and self transitions`(from: CycleStatus, to: CycleStatus) {
            assertFalse(from.canMoveTo(to))
        }

        @Test
        fun `safeValueOf reads stored names and tolerates unknown ones`() {
            assertEquals(CycleStatus.PARTIALLY_PAID, CycleStatus.safeValueOf("PARTIALLY_PAID"))
            assertNull(CycleStatus.safeValueOf("SETTLED"))
        }
    }

    @Nested
    @DisplayName("CycleDueBreakdown")
    inner class Breakdown {

        @Test
        fun `saldo card owes purchases minus refunds`() {
            assertEquals(100.0, CycleDueBreakdown(purchases = 120.50, credits = 20.50).totalDue)
        }

        @Test
        fun `amex hybrid owes the cycle net of pagoflex plus installments revolving and interest`() {
            val breakdown = CycleDueBreakdown(
                purchases = 1000.0,
                movedToInstallments = 600.0,
                installmentsDue = 100.0,
                revolvingDue = 50.0,
                interest = 3.21,
            )

            assertEquals(553.21, breakdown.totalDue)
        }

        @Test
        fun `total is rounded to the cent`() {
            assertEquals(0.3, CycleDueBreakdown(purchases = 0.1, installmentsDue = 0.2).totalDue)
        }

        @Test
        fun `refunds larger than purchases never produce a negative due`() {
            assertEquals(0.0, CycleDueBreakdown(purchases = 10.0, credits = 30.0).totalDue)
        }

        @Test
        fun `rejects negative or non finite items and pagoflex beyond purchases`() {
            assertThrows<IllegalArgumentException> { CycleDueBreakdown(purchases = -1.0) }
            assertThrows<IllegalArgumentException> { CycleDueBreakdown(purchases = Double.NaN) }
            assertThrows<IllegalArgumentException> { CycleDueBreakdown(purchases = 10.0, interest = Double.POSITIVE_INFINITY) }
            assertThrows<IllegalArgumentException> { CycleDueBreakdown(purchases = 100.0, movedToInstallments = 100.01) }
        }
    }

    @Nested
    @DisplayName("BillingCycle")
    inner class Cycle {

        private fun cycle(
            closingDate: LocalDate = LocalDate.of(2025, 3, 20),
            dueDate: LocalDate = LocalDate.of(2025, 4, 15),
            status: CycleStatus = CycleStatus.OPEN,
            paymentMode: CyclePaymentMode = CyclePaymentMode.SALDO,
            chosenAmount: Double? = null,
            settlementTransactionId: String? = null,
            carriedForward: Double? = null,
        ) = BillingCycle(
            id = "cycle1",
            paymentMethodId = "card1",
            cycleMonth = YearMonth.of(2025, 3),
            closingDate = closingDate,
            dueDate = dueDate,
            status = status,
            paymentMode = paymentMode,
            chosenAmount = chosenAmount,
            settlementTransactionId = settlementTransactionId,
            carriedForward = carriedForward,
        )

        @Test
        fun `builds a paid cycle with its settlement transaction`() {
            val paid = cycle(status = CycleStatus.PAID, settlementTransactionId = "tx1")

            assertEquals("tx1", paid.settlementTransactionId)
        }

        @Test
        fun `allows due date equal to closing when february clamps both days`() {
            val february = BillingCycle(
                id = "cycle1",
                paymentMethodId = "card1",
                cycleMonth = YearMonth.of(2025, 2),
                closingDate = LocalDate.of(2025, 2, 28),
                dueDate = LocalDate.of(2025, 2, 28),
                status = CycleStatus.OPEN,
            )

            assertEquals(february.closingDate, february.dueDate)
        }

        @Test
        fun `rejects a closing date outside the cycle month`() {
            assertThrows<IllegalArgumentException> { cycle(closingDate = LocalDate.of(2025, 4, 1), dueDate = LocalDate.of(2025, 4, 15)) }
        }

        @Test
        fun `rejects a due date before closing`() {
            assertThrows<IllegalArgumentException> { cycle(dueDate = LocalDate.of(2025, 3, 19)) }
        }

        @Test
        fun `fixed mode needs a chosen amount`() {
            assertThrows<IllegalArgumentException> { cycle(paymentMode = CyclePaymentMode.FIXED) }
            assertEquals(200.0, cycle(paymentMode = CyclePaymentMode.FIXED, chosenAmount = 200.0).chosenAmount)
        }

        @Test
        fun `rejects a settlement on a cycle that is not paid`() {
            assertThrows<IllegalArgumentException> { cycle(status = CycleStatus.OPEN, settlementTransactionId = "tx1") }
            assertThrows<IllegalArgumentException> { cycle(status = CycleStatus.CLOSED, settlementTransactionId = "tx1") }
        }

        @Test
        fun `rejects negative amounts`() {
            assertThrows<IllegalArgumentException> { cycle(chosenAmount = -5.0) }
            assertThrows<IllegalArgumentException> { cycle(carriedForward = -5.0) }
        }
    }
}
