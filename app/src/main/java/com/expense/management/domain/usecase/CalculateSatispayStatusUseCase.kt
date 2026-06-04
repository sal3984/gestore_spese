package com.expense.management.domain.usecase

import com.expense.management.data.ExpenseRepository
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.SatispayDetailEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.SatispayStatus
import java.time.DayOfWeek
import java.time.LocalDate

class CalculateSatispayStatusUseCase(private val repository: ExpenseRepository) {

    suspend fun execute(method: PaymentMethodEntity, detail: SatispayDetailEntity): SatispayStatus {
        val today = LocalDate.now()
        val currentWeekStart = getCurrentWeekStart(today, detail.sddDay)
        val transactions = repository.getAllTransactionsList()
            .filter { tx ->
                tx.paymentMethodId == method.id &&
                    tx.type == TransactionType.EXPENSE &&
                    tx.date >= currentWeekStart.toString() &&
                    tx.date <= today.toString()
            }
        val spent = transactions.sumOf { it.amount }
        return SatispayStatus(
            methodName = method.name,
            weeklyBudget = detail.weeklyBudget,
            spentThisWeek = spent,
            remaining = detail.weeklyBudget - spent,
        )
    }

    private fun getCurrentWeekStart(today: LocalDate, sddDay: Int): LocalDate {
        val targetDay = DayOfWeek.of(sddDay.coerceIn(1, 7))
        var start = today
        while (start.dayOfWeek != targetDay) {
            start = start.minusDays(1)
        }
        return start
    }
}
