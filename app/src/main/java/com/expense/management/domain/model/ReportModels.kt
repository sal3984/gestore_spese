package com.expense.management.domain.model

import java.time.LocalDate
import java.time.YearMonth

data class ReportData(
    val savings: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val monthlyBalances: List<ReportMonthlyBalance>,
    val expenseByCategory: List<ReportCategorySummary>,
    val totalMonthlyExpense: Double,
) {
    companion object {
        val EMPTY = ReportData(
            savings = 0.0,
            totalIncome = 0.0,
            totalExpense = 0.0,
            monthlyBalances = emptyList(),
            expenseByCategory = emptyList(),
            totalMonthlyExpense = 0.0,
        )
    }
}

data class ReportMonthlyBalance(
    val month: YearMonth,
    val balance: Double,
)

data class ReportCategorySummary(
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val amount: Double,
    val percentage: Float,
)

data class ReportTransaction(
    val description: String,
    val amount: Double,
    val date: LocalDate,
    val isExpense: Boolean,
    val paymentMethodName: String?,
)
