package com.expense.management.domain.model

data class AmexForecastMonth(
    val monthOffset: Int,
    val monthLabel: String,
    val pagoflexQuota: Double,
    val revolvingPayment: Double,
    val interest: Double,
    val carriedForwardAfter: Double,
    val totalDue: Double,
)
