package com.expense.management.domain.model

/**
 * Regole di fatturazione di una carta: giorno di chiusura, giorno di addebito e di quanti
 * mesi l'addebito segue la chiusura. Un giorno oltre la fine del mese vale l'ultimo giorno.
 */
data class CardCycleRules(
    val closingDay: Int,
    val dueDay: Int,
    val dueMonthOffset: Int,
) {
    init {
        require(closingDay in DAY_RANGE) { "closingDay fuori range: $closingDay" }
        require(dueDay in DAY_RANGE) { "dueDay fuori range: $dueDay" }
        require(dueMonthOffset in 0..MAX_DUE_MONTH_OFFSET) { "dueMonthOffset fuori range: $dueMonthOffset" }
        require(dueMonthOffset > 0 || dueDay >= closingDay) {
            "addebito il $dueDay nello stesso mese della chiusura il $closingDay"
        }
    }

    companion object {
        private val DAY_RANGE = 1..31
        private const val MAX_DUE_MONTH_OFFSET = 2

        /** Giorno di chiusura o di addebito scritto dall'utente: valido solo se intero da 1 a 31. */
        fun parseCardDay(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in DAY_RANGE }

        /**
         * Regole dai due giorni salvati oggi sulla carta, con la stessa logica di
         * DateUtils.calculateEffectiveDate: addebito nel mese della chiusura se il giorno
         * di addebito non la precede, altrimenti nel mese dopo.
         * Null se la carta non ha un ciclo configurato (giorni a 0). Le carte salvate prima della
         * validazione possono avere giorni oltre il 31: valgono fine mese, ma il mese di addebito
         * si decide sui valori salvati.
         */
        fun fromCardDays(closingDay: Int, paymentDay: Int): CardCycleRules? {
            if (closingDay < DAY_RANGE.first || paymentDay < DAY_RANGE.first) return null
            return CardCycleRules(
                closingDay = closingDay.coerceAtMost(DAY_RANGE.last),
                dueDay = paymentDay.coerceAtMost(DAY_RANGE.last),
                dueMonthOffset = if (paymentDay >= closingDay) 0 else 1,
            )
        }
    }
}
