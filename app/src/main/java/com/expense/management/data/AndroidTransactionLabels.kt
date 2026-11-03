package com.expense.management.data

import android.content.Context
import com.expense.management.R
import com.expense.management.domain.model.TransactionLabels

/** Descrizioni generate lette dalle risorse, nella lingua corrente dell'app a ogni chiamata. */
class AndroidTransactionLabels(context: Context) : TransactionLabels {
    private val context = context.applicationContext

    override fun cardCharge(cardName: String) = context.getString(R.string.tx_card_charge, cardName).trim()
    override fun cardInstallment(cardName: String) = context.getString(R.string.tx_card_installment, cardName).trim()
    override fun limitRestore(cardName: String) = context.getString(R.string.tx_limit_restore, cardName)
    override fun amexInstallment(sequenceNumber: Int, installmentCount: Int?) =
        context.getString(R.string.tx_amex_installment, sequenceNumber, installmentCount?.toString() ?: "-")
    override fun amexStatementPayment(statementMonth: String) = context.getString(R.string.tx_amex_statement_payment, statementMonth)
    override fun amexStatementRefund(statementMonth: String) = context.getString(R.string.tx_amex_statement_refund, statementMonth)
    override val unnamedCard: String get() = context.getString(R.string.tx_unnamed_card)
    override val futurePayment: String get() = context.getString(R.string.tx_future_payment)
}
