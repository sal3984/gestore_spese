package com.expense.management.domain.model

data class CurrentAccountCashFlow(
    val income: Double,
    val outflows: Double,
) {
    val net: Double get() = income - outflows
}
