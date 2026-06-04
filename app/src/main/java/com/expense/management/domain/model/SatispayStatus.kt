package com.expense.management.domain.model

data class SatispayStatus(
    val methodName: String,
    val weeklyBudget: Double,
    val spentThisWeek: Double,
    val remaining: Double,
)
