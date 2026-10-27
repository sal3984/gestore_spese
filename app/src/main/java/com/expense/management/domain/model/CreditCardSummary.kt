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
    // Solo carte a saldo con ciclo: plafond disponibile e prossimo addebito (null se nessuno)
    val availableCredit: Double? = null,
    val nextCharge: CycleCharge? = null,
)
