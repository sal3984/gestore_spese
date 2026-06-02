package com.expense.management.domain.usecase

import com.expense.management.data.CategoryEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.ReportCategorySummary
import com.expense.management.domain.model.ReportData
import com.expense.management.domain.model.ReportMonthlyBalance
import java.time.LocalDate
import java.time.YearMonth

class CalculateReportUseCase {

    fun execute(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        start: YearMonth,
        end: YearMonth,
    ): ReportData {
        val dateCache = HashMap<String, LocalDate>(transactions.size)
        val monthIncome = HashMap<YearMonth, Double>()
        val monthExpense = HashMap<YearMonth, Double>()
        val categoryExpense = HashMap<String, Double>()
        var totalIncome = 0.0
        var totalExpense = 0.0

        for (tx in transactions) {
            val ym = YearMonth.from(
                dateCache.getOrPut(tx.effectiveDate) {
                    LocalDate.parse(tx.effectiveDate)
                },
            )

            if (tx.type == TransactionType.EXPENSE) {
                totalExpense += tx.amount
                categoryExpense.merge(tx.categoryId, tx.amount, Double::plus)
                monthExpense.merge(ym, tx.amount, Double::plus)
            } else {
                totalIncome += tx.amount
                monthIncome.merge(ym, tx.amount, Double::plus)
            }
        }

        val monthlyBalances = mutableListOf<ReportMonthlyBalance>()
        var current = start
        while (!current.isAfter(end)) {
            monthlyBalances.add(
                ReportMonthlyBalance(
                    month = current,
                    balance = (monthIncome[current] ?: 0.0) - (monthExpense[current] ?: 0.0),
                ),
            )
            current = current.plusMonths(1)
        }

        val totalMonthlyExpense = categoryExpense.values.sum()

        val categoriesById = categories.associateBy { it.id }
        val otherCategory = categories.firstOrNull { it.id == "other" }

        val expenseByCategory = categoryExpense.entries
            .sortedByDescending { it.value }
            .map { entry ->
                val category = categoriesById[entry.key] ?: otherCategory
                val categoryName = category?.label ?: entry.key
                val categoryIcon = category?.icon ?: "\uD83C\uDFF7\uFE0F"
                val percentage = if (totalMonthlyExpense > 0) (entry.value / totalMonthlyExpense).toFloat() else 0f
                ReportCategorySummary(
                    categoryId = entry.key,
                    categoryName = categoryName,
                    categoryIcon = categoryIcon,
                    amount = entry.value,
                    percentage = percentage,
                )
            }

        return ReportData(
            savings = totalIncome - totalExpense,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            monthlyBalances = monthlyBalances,
            expenseByCategory = expenseByCategory,
            totalMonthlyExpense = totalMonthlyExpense,
        )
    }
}
