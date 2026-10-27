package com.expense.management.domain.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * Ciclo di fatturazione di una carta di credito, unico per SALDO, REVOLVING, INSTALLMENT
 * e AMEX_HYBRID. Non contiene totali: il dovuto si ricava dalle transazioni del ciclo.
 * [settlementTransactionId] è l'unico addebito sul conto corrente che salda il ciclo.
 */
data class BillingCycle(
    val id: String,
    val paymentMethodId: String,
    val cycleMonth: YearMonth,
    val closingDate: LocalDate,
    val dueDate: LocalDate,
    val status: CycleStatus,
    val paymentMode: CyclePaymentMode = CyclePaymentMode.SALDO,
    val chosenAmount: Double? = null,
    val settlementTransactionId: String? = null,
    val carriedForward: Double? = null,
) {
    init {
        require(YearMonth.from(closingDate) == cycleMonth) {
            "chiusura $closingDate fuori dal mese del ciclo $cycleMonth"
        }
        // Uguali sono ammessi: chiusura il 30 e addebito il 31 cadono entrambi il 28 a febbraio
        require(!dueDate.isBefore(closingDate)) { "addebito $dueDate prima della chiusura $closingDate" }
        require(chosenAmount == null || (chosenAmount >= 0.0 && chosenAmount.isFinite())) {
            "importo scelto non valido: $chosenAmount"
        }
        require(paymentMode != CyclePaymentMode.FIXED || chosenAmount != null) {
            "la modalità FIXED richiede un importo scelto"
        }
        require(carriedForward == null || (carriedForward >= 0.0 && carriedForward.isFinite())) {
            "riporto non valido: $carriedForward"
        }
        require(settlementTransactionId == null || status == CycleStatus.PARTIALLY_PAID || status == CycleStatus.PAID) {
            "addebito di saldo su un ciclo $status"
        }
    }
}
