package com.expense.management.domain.usecase

import com.expense.management.domain.model.TransactionLabels

/** Testi riconoscibili per verificare che i use case usino le etichette ricevute e non testi propri. */
object FakeTransactionLabels : TransactionLabels {
    override fun cardCharge(cardName: String) = "charge:$cardName"
    override fun cardInstallment(cardName: String) = "installment:$cardName"
    override fun limitRestore(cardName: String) = "restore:$cardName"
    override fun amexInstallment(sequenceNumber: Int, installmentCount: Int?) = "amex:$sequenceNumber/$installmentCount"
    override fun amexStatementPayment(statementMonth: String) = "amexPay:$statementMonth"
    override fun amexStatementRefund(statementMonth: String) = "amexRefund:$statementMonth"
    override val unnamedCard = "noCard"
    override val futurePayment = "(future)"
}
