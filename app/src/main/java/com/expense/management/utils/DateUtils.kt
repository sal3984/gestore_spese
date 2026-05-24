package com.expense.management.utils

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object DateUtils {

    data class CardDateInfo(
        val closingDay: Int,
        val paymentDay: Int,
    )

    fun calculateEffectiveDate(transactionDate: LocalDate, card: CardDateInfo): String {
        // Se la carta non ha giorni specifici, si addebita subito (nessun ritardo)
        if (card.closingDay <= 0 || card.paymentDay <= 0) {
            return transactionDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        }

        val closingDay = card.closingDay
        val paymentDay = card.paymentDay
        val transactionYearMonth = YearMonth.from(transactionDate)
        val effectiveClosing = closingDay.coerceAtMost(transactionYearMonth.lengthOfMonth())

        var paymentMonth = transactionYearMonth

        if (transactionDate.dayOfMonth >= effectiveClosing) {
            paymentMonth = paymentMonth.plusMonths(1)
        }

        var finalPaymentDate = getValidDateForDay(paymentMonth, paymentDay)

        if (!finalPaymentDate.isAfter(transactionDate)) {
            paymentMonth = paymentMonth.plusMonths(1)
            finalPaymentDate = getValidDateForDay(paymentMonth, paymentDay)
        }

        return finalPaymentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /**
     * Restituisce una `LocalDate` valida per un dato mese, gestendo i giorni di fine mese.
     * Se il giorno richiesto (es. 31) non esiste in quel mese, restituisce l'ultimo giorno valido.
     */
    private fun getValidDateForDay(yearMonth: YearMonth, day: Int): LocalDate {
        val lastDayOfMonth = yearMonth.lengthOfMonth()
        val validDay = if (day > lastDayOfMonth) lastDayOfMonth else day
        return yearMonth.atDay(validDay)
    }
}
