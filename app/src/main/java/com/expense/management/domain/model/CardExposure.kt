package com.expense.management.domain.model

/** Utilizzo del plafond: quanto la carta deve ancora e quanto resta disponibile. */
data class CardExposure(
    val exposure: Double,
    val available: Double,
    val utilization: Float,
)
