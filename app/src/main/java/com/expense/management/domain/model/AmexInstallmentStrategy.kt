package com.expense.management.domain.model

sealed interface AmexInstallmentStrategy {
    data class FixedAmount(val amount: Double) : AmexInstallmentStrategy
    data class FixedDuration(val months: Int) : AmexInstallmentStrategy
}
