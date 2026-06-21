package com.expense.management.domain.usecase

import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity
import com.expense.management.domain.model.AmexInstallmentStrategy
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.round

class BuildAmexScheduledPaymentsUseCase {

    fun execute(
        planId: String,
        totalAmount: Double,
        strategy: AmexInstallmentStrategy,
        startDate: String,
        paymentDay: Int? = null,
    ): List<AmexPagoFlexScheduledPaymentEntity> {
        if (totalAmount <= 0.0) {
            return emptyList()
        }

        val start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE)
        val day = paymentDay?.coerceAtMost(28) ?: start.dayOfMonth.coerceAtMost(28)

        val (count, regularAmount, lastAmount) = when (strategy) {
            is AmexInstallmentStrategy.FixedAmount -> {
                require(strategy.amount > 0.0) { "Importo rata deve essere maggiore di zero" }
                val c = ceil(totalAmount / strategy.amount).toInt()
                val regular = round2(strategy.amount)
                val totalRegular = regular * (c - 1)
                val last = round2(totalAmount - totalRegular)
                Triple(c, regular, last)
            }
            is AmexInstallmentStrategy.FixedDuration -> {
                require(strategy.months > 0) { "Durata deve essere maggiore di zero" }
                val c = strategy.months
                val regular = round2(totalAmount / c)
                val totalRegular = regular * (c - 1)
                val last = round2(totalAmount - totalRegular)
                Triple(c, regular, last)
            }
        }

        return (0 until count).map { index ->
            val dueDate = start.plusMonths(index.toLong()).withDayOfMonth(day)
            val amount = if (index == count - 1) lastAmount else regularAmount
            AmexPagoFlexScheduledPaymentEntity(
                id = UUID.randomUUID().toString(),
                planId = planId,
                sequenceNumber = index + 1,
                dueDate = dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                amount = amount,
                status = "PENDING",
                expenseTransactionId = null,
            )
        }
    }

    private fun round2(value: Double): Double = round(value * 100.0) / 100.0
}
