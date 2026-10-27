package com.expense.management.domain.model

/**
 * Come l'utente sceglie di saldare un ciclo. Stessi nomi di [AmexPaymentMode],
 * così i valori già salvati in amex_statements.paymentMode si copiano senza conversione.
 */
enum class CyclePaymentMode {
    SALDO,
    MINIMUM,
    FIXED,
    PAGOFLEX_ONLY,
    ;

    companion object {
        fun safeValueOf(name: String): CyclePaymentMode? =
            entries.find { it.name == name }
    }
}
