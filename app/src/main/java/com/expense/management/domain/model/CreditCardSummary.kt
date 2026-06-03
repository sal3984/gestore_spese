package com.expense.management.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class CreditCardSummary(
    val cardId: String,
    val name: String,
    val limit: Double,
    val cardType: CreditCardType,
    val displayedSpent: Double,
    val totalUtilized: Double,
    val totalPaid: Double,
    val progress: Float,
)
