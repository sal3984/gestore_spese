package com.expense.management.domain.model

/**
 * Descrizioni delle transazioni create dall'app (addebiti, rate, ripristini, righe speculari).
 * Si scrivono nella lingua del telefono al momento della creazione e restano salvate cosi':
 * l'app le fornisce dalle risorse, il dominio non dipende da Android.
 */
interface TransactionLabels {
    fun cardCharge(cardName: String): String
    fun cardInstallment(cardName: String): String
    fun limitRestore(cardName: String): String
    fun amexInstallment(sequenceNumber: Int, installmentCount: Int?): String
    fun amexStatementPayment(statementMonth: String): String
    fun amexStatementRefund(statementMonth: String): String

    /** Nome usato nelle righe speculari quando la carta non e' piu' nota. */
    val unnamedCard: String

    /** Suffisso della riga speculare sul conto di un'entrata su carta. */
    val futurePayment: String

    companion object {
        /** Testi usati prima della localizzazione: ripiego per i test e per chi non passa le risorse. */
        val Default: TransactionLabels = object : TransactionLabels {
            override fun cardCharge(cardName: String) = "Addebito $cardName".trim()
            override fun cardInstallment(cardName: String) = "Rata $cardName".trim()
            override fun limitRestore(cardName: String) = "[$cardName] Ripristino plafond"
            override fun amexInstallment(sequenceNumber: Int, installmentCount: Int?) = "Rata Amex $sequenceNumber/${installmentCount ?: "-"}"
            override fun amexStatementPayment(statementMonth: String) = "Pagamento Amex $statementMonth"
            override fun amexStatementRefund(statementMonth: String) = "Rimborso Amex $statementMonth"
            override val unnamedCard = "Credit Card"
            override val futurePayment = "(Future Payment)"
        }
    }
}
