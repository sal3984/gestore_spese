package com.expense.management.utils

import com.expense.management.domain.model.CardCycleRules
import com.expense.management.domain.usecase.CalculateBillingCycleUseCase
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtils {

    data class CardDateInfo(
        val closingDay: Int,
        val paymentDay: Int,
    )

    private val calculateBillingCycle = CalculateBillingCycleUseCase()

    /**
     * Data di addebito di un acquisto con carta: la data di addebito del suo ciclo.
     * Senza ciclo configurato (giorni a 0) l'addebito e' immediato.
     */
    fun calculateEffectiveDate(transactionDate: LocalDate, card: CardDateInfo): String {
        val rules = CardCycleRules.fromCardDays(card.closingDay, card.paymentDay)
            ?: return transactionDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return calculateBillingCycle.execute(transactionDate, rules).dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}
