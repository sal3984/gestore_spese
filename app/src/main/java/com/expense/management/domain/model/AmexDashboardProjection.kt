package com.expense.management.domain.model

data class AmexDashboardProjection(
    val paymentMethodId: String,
    val cardName: String,
    val targetMonth: String,
    val pagoflexQuotaTotal: Double,
    val pagoflexPlanCount: Int,
    val hasDuePayment: Boolean,
    val duePaymentAmount: Double,
    val hasOpenStatement: Boolean,
)
