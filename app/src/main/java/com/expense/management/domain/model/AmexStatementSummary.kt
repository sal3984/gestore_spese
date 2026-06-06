package com.expense.management.domain.model

data class AmexStatementSummary(
    val statementId: String,
    val paymentMethodId: String,
    val statementMonth: String,
    val totalExpenses: Double,
    val totalPagoflex: Double,
    val revolvingEligible: Double,
    val pagoflexQuota: Double,
    val paymentMode: AmexPaymentMode,
    val paymentAmount: Double,
    val carriedForward: Double,
    val interestRate: Double,
)
