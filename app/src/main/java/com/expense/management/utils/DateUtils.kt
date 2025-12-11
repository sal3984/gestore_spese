package com.expense.management.utils

import com.expense.management.data.CreditCardEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object DateUtils {

    /**
     * Calcola la data di addebito effettiva per una transazione con carta di credito.
     *
     * @param transactionDate La data in cui è avvenuta la transazione.
     * @param card La carta di credito usata, che contiene i dettagli su chiusura e pagamento.
     * @return La data di addebito calcolata come stringa in formato ISO (YYYY-MM-DD).
     */
    fun calculateEffectiveDate(transactionDate: LocalDate, card: CreditCardEntity): String {
        // Se la carta non ha giorni specifici, si addebita subito (nessun ritardo)
        if (card.closingDay <= 0 || card.paymentDay <= 0) {
            return transactionDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        }

        val closingDay = card.closingDay
        val paymentDay = card.paymentDay

        var paymentMonth = YearMonth.from(transactionDate)

        // Se la transazione avviene DOPO o il giorno stesso della chiusura,
        // l'addebito slitta al ciclo di fatturazione successivo.
        if (transactionDate.dayOfMonth >= closingDay) {
            paymentMonth = paymentMonth.plusMonths(1)
        }

        // Calcola la data di pagamento finale, gestendo i giorni di fine mese
        val finalPaymentDate = getValidDateForDay(paymentMonth, paymentDay)

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
