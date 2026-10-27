package com.expense.management.domain.usecase

import com.expense.management.domain.model.BillingCycle
import com.expense.management.domain.model.CardCycleRules
import com.expense.management.domain.model.CycleStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import java.time.YearMonth

@DisplayName("CalculateBillingCycleUseCase")
class CalculateBillingCycleUseCaseTest {

    private val useCase = CalculateBillingCycleUseCase()

    @ParameterizedTest(name = "{0}: acquisto {1} -> ciclo {4}, chiusura {5}, addebito {6}")
    @CsvSource(
        "prima della chiusura,              2025-03-10, 15:5:1,  2025-03, 2025-03-15, 2025-04-05",
        "il giorno di chiusura,             2025-03-15, 15:5:1,  2025-03, 2025-03-15, 2025-04-05",
        "il giorno dopo la chiusura,        2025-03-16, 15:5:1,  2025-04, 2025-04-15, 2025-05-05",
        "fine mese a febbraio,              2025-02-28, 31:15:1, 2025-02, 2025-02-28, 2025-03-15",
        "fine mese - primo di marzo,        2025-03-01, 31:15:1, 2025-03, 2025-03-31, 2025-04-15",
        "febbraio bisestile,                2024-02-29, 31:15:1, 2024-02, 2024-02-29, 2024-03-15",
        "chiusura 30 a febbraio bisestile,  2024-02-29, 30:10:1, 2024-02, 2024-02-29, 2024-03-10",
        "chiusura 30 - 31 gennaio,           2025-01-31, 30:10:1, 2025-02, 2025-02-28, 2025-03-10",
        "fine mese ad aprile,               2025-04-30, 31:15:1, 2025-04, 2025-04-30, 2025-05-15",
        "addebito 31 in mese da 30,         2025-03-05, 10:31:1, 2025-03, 2025-03-10, 2025-04-30",
        "addebito nello stesso mese,        2025-03-05, 10:25:0, 2025-03, 2025-03-10, 2025-03-25",
        "cambio d'anno,                     2025-12-21, 20:15:1, 2026-01, 2026-01-20, 2026-02-15",
        "dicembre con addebito a gennaio,   2025-12-10, 20:15:1, 2025-12, 2025-12-20, 2026-01-15",
        "addebito a due mesi,               2025-03-10, 15:5:2,  2025-03, 2025-03-15, 2025-05-05",
        "chiusura il primo - acquisto il 1,  2025-03-01, 1:20:0,  2025-03, 2025-03-01, 2025-03-20",
        "chiusura il primo - acquisto il 2,  2025-03-02, 1:20:0,  2025-04, 2025-04-01, 2025-04-20",
        "chiusura e addebito a fine feb,    2025-02-10, 30:31:0, 2025-02, 2025-02-28, 2025-02-28",
    )
    fun `computes the cycle of a purchase`(
        case: String,
        purchase: String,
        rules: String,
        cycleMonth: String,
        closingDate: String,
        dueDate: String,
    ) {
        val (closingDay, dueDay, offset) = rules.split(":").map { it.toInt() }

        val result = useCase.execute(LocalDate.parse(purchase), CardCycleRules(closingDay, dueDay, offset))

        assertEquals(
            CalculateBillingCycleUseCase.CycleDates(YearMonth.parse(cycleMonth), LocalDate.parse(closingDate), LocalDate.parse(dueDate)),
            result,
            case,
        )
    }

    @Test
    fun `matches the original DateUtils everywhere except purchases on the closing day`() {
        // Due anni di acquisti (bisestile compreso) per ogni coppia di giorni 1..31
        val purchases = generateSequence(LocalDate.of(2024, 1, 1)) { it.plusDays(1) }
            .takeWhile { it.year < 2026 }
            .toList()
        for (closingDay in 1..31) {
            for (paymentDay in 1..31) {
                val rules = requireNotNull(CardCycleRules.fromCardDays(closingDay, paymentDay))
                for (purchase in purchases) {
                    val actual = useCase.execute(purchase, rules).dueDate
                    val month = YearMonth.from(purchase)
                    val onClosingDay = purchase.dayOfMonth == minOf(closingDay, month.lengthOfMonth())
                    // Il giorno di chiusura ora entra nel ciclo che chiude, come il giorno prima
                    val reference = if (onClosingDay) purchase.minusDays(1) else purchase
                    val expected = legacyEffectiveDate(reference, closingDay, paymentDay)

                    assertEquals(expected, actual, "chiusura $closingDay, addebito $paymentDay, acquisto $purchase")
                }
            }
        }
    }

    @Test
    fun `every purchase falls between the previous closing and its own, and builds a valid cycle`() {
        val sampleRules = listOf(CardCycleRules(15, 5, 1), CardCycleRules(31, 15, 1), CardCycleRules(30, 31, 0), CardCycleRules(1, 20, 0), CardCycleRules(28, 10, 2))
        var purchase = LocalDate.of(2024, 1, 1)
        while (purchase.year < 2026) {
            for (rules in sampleRules) {
                val dates = useCase.execute(purchase, rules)
                val previousClosing = useCase.execute(dates.closingDate.minusMonths(1).withDayOfMonth(1), rules).closingDate

                assertTrue(!purchase.isAfter(dates.closingDate), "$rules: $purchase dopo la chiusura ${dates.closingDate}")
                assertTrue(purchase.isAfter(previousClosing), "$rules: $purchase non dopo la chiusura precedente $previousClosing")
                BillingCycle(
                    id = "c",
                    paymentMethodId = "card",
                    cycleMonth = dates.cycleMonth,
                    closingDate = dates.closingDate,
                    dueDate = dates.dueDate,
                    status = CycleStatus.OPEN,
                )
            }
            purchase = purchase.plusDays(1)
        }
    }

    @Test
    fun `purchases of the same cycle share closing and due date`() {
        val rules = CardCycleRules(20, 15, 1)
        val cycle = (21..31).map { useCase.execute(LocalDate.of(2025, 1, it), rules) } +
            (1..20).map { useCase.execute(LocalDate.of(2025, 2, it), rules) }

        assertEquals(1, cycle.toSet().size)
        assertEquals(LocalDate.of(2025, 3, 15), cycle.first().dueDate)
    }

    /** Copia del DateUtils.calculateEffectiveDate precedente all'1.2, come riferimento. */
    private fun legacyEffectiveDate(transactionDate: LocalDate, closingDay: Int, paymentDay: Int): LocalDate {
        val transactionYearMonth = YearMonth.from(transactionDate)
        val effectiveClosing = closingDay.coerceAtMost(transactionYearMonth.lengthOfMonth())
        val closingMonth = if (transactionDate.dayOfMonth >= effectiveClosing) transactionYearMonth.plusMonths(1) else transactionYearMonth
        val paymentMonth = if (paymentDay >= closingDay) closingMonth else closingMonth.plusMonths(1)
        var finalPaymentDate = paymentMonth.atDay(minOf(paymentDay, paymentMonth.lengthOfMonth()))
        if (!finalPaymentDate.isAfter(transactionDate)) {
            val adjustedMonth = paymentMonth.plusMonths(1)
            finalPaymentDate = adjustedMonth.atDay(minOf(paymentDay, adjustedMonth.lengthOfMonth()))
        }
        return finalPaymentDate
    }
}
