package com.expense.management.domain.model

data class CreditCardSummary(
    val cardId: String,
    val name: String,
    val limit: Double,
    val cardType: CreditCardType,
    val displayedSpent: Double,
    val totalUtilized: Double,
    val totalPaid: Double,
    val totalRepaid: Double = 0.0,
    val progress: Float,
)
