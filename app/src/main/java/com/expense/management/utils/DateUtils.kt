package com.expense.management.utils

import com.expense.management.domain.model.CardCycleRules
import com.expense.management.domain.usecase.CalculateBillingCycleUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtils {

    data class CardDateInfo(
        val closingDay: Int,
        val paymentDay: Int,
    )

    private val calculateBillingCycle = CalculateBillingCycleUseCase()

    /**
     * La data di oggi, riemessa quando cambia. La data si legge alla sottoscrizione e poi a ogni
     * controllo: il processo dell'app puo' restare vivo per giorni, e un'attesa calcolata fino a
     * mezzanotte slitterebbe mentre il dispositivo dorme.
     */
    fun currentDateFlow(
        today: () -> LocalDate = { LocalDate.now() },
        checkEveryMillis: Long = 60_000L,
    ): Flow<LocalDate> = flow {
        while (true) {
            emit(today())
            delay(checkEveryMillis)
        }
    }.distinctUntilChanged()

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
