package com.expense.management.domain.model

data class ActiveCreditCard(
    val id: String,
    val name: String,
    val provider: PaymentProvider,
    val cardType: CreditCardType,
    val limit: Double,
    val closingDay: Int,
    val paymentDay: Int,
    val linkedPaymentMethodId: String? = null,
) {
    /**
     * Carta a saldo con ciclo: e' l'addebito del ciclo a muovere il conto e a fissare la data di
     * addebito di ogni movimento, rate comprese. Un'entrata sulla carta (rimborso) riduce l'addebito,
     * quindi non serve la riga speculare in uscita sul conto.
     */
    val settlesThroughCycles: Boolean
        get() = cardType == CreditCardType.SALDO && CardCycleRules.fromCardDays(closingDay, paymentDay) != null
}
