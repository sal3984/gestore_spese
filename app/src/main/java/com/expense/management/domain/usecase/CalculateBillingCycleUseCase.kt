package com.expense.management.domain.usecase

import com.expense.management.domain.model.CardCycleRules
import java.time.LocalDate
import java.time.YearMonth

/**
 * Ciclo di fatturazione di un acquisto: il ciclo chiude nel primo giorno di chiusura che non
 * precede l'acquisto, quindi un acquisto fatto il giorno di chiusura entra nel ciclo che chiude.
 * Tutti gli acquisti dello stesso ciclo hanno la stessa data di addebito.
 * Un giorno oltre la fine del mese vale l'ultimo giorno (31 = fine mese, anche a febbraio).
 */
class CalculateBillingCycleUseCase {

    data class CycleDates(
        val cycleMonth: YearMonth,
        val closingDate: LocalDate,
        val dueDate: LocalDate,
    )

    fun execute(purchaseDate: LocalDate, rules: CardCycleRules): CycleDates {
        val purchaseMonth = YearMonth.from(purchaseDate)
        val cycleMonth = if (purchaseDate.isAfter(purchaseMonth.dayOrLast(rules.closingDay))) {
            purchaseMonth.plusMonths(1)
        } else {
            purchaseMonth
        }
        val dueMonth = cycleMonth.plusMonths(rules.dueMonthOffset.toLong())
        return CycleDates(
            cycleMonth = cycleMonth,
            closingDate = cycleMonth.dayOrLast(rules.closingDay),
            dueDate = dueMonth.dayOrLast(rules.dueDay),
        )
    }

    private fun YearMonth.dayOrLast(day: Int): LocalDate = atDay(minOf(day, lengthOfMonth()))
}
