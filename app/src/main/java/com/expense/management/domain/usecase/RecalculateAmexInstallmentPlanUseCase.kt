package com.expense.management.domain.usecase

import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity
import com.expense.management.domain.model.AmexInstallmentStrategy
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.round

class RecalculateAmexInstallmentPlanUseCase(
    private val buildPaymentsUseCase: BuildAmexScheduledPaymentsUseCase = BuildAmexScheduledPaymentsUseCase(),
) {

    fun execute(
        existingPlan: AmexPagoFlexPlanEntity,
        existingPayments: List<AmexPagoFlexScheduledPaymentEntity>,
        newStrategy: AmexInstallmentStrategy,
        today: String,
        currentStatementPaymentDueDate: String,
    ): Pair<AmexPagoFlexPlanEntity, List<AmexPagoFlexScheduledPaymentEntity>> {
        val todayDate = LocalDate.parse(today, DateTimeFormatter.ISO_LOCAL_DATE)
        val dueDate = LocalDate.parse(currentStatementPaymentDueDate, DateTimeFormatter.ISO_LOCAL_DATE)

        require(existingPlan.totalAmount > 0.0) { "Importo totale deve essere maggiore di zero" }
        val paidPayments = existingPayments
            .filter { it.status == "PAID" }
            .sortedBy { it.sequenceNumber }

        val pendingPayments = existingPayments
            .filter { it.status != "PAID" }
            .sortedBy { it.sequenceNumber }

        val paidCount = paidPayments.size
        val paidAmount = paidPayments.sumOf { it.amount }
        val residual = round2(existingPlan.totalAmount - paidAmount)

        if (pendingPayments.isEmpty() || residual <= 0.0) {
            val (newPlanType, newInitialAmount) = when (newStrategy) {
                is AmexInstallmentStrategy.FixedAmount -> Pair("FIXED_AMOUNT", newStrategy.amount)
                is AmexInstallmentStrategy.FixedDuration -> Pair("FIXED_DURATION", null)
            }
            return Pair(
                existingPlan.copy(
                    installmentCount = paidCount,
                    installmentAmount = 0.0,
                    planType = newPlanType,
                    initialInstallmentAmount = newInitialAmount,
                ),
                emptyList(),
            )
        }

        val firstPendingDate = LocalDate.parse(
            pendingPayments.first().dueDate,
            DateTimeFormatter.ISO_LOCAL_DATE,
        )

        val effectiveStartDate = if (todayDate.isBefore(dueDate)) {
            firstPendingDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        } else {
            firstPendingDate.plusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        }

        val newPendingPayments = buildPaymentsUseCase.execute(
            planId = existingPlan.id,
            totalAmount = residual,
            strategy = newStrategy,
            startDate = effectiveStartDate,
        ).mapIndexed { index, payment ->
            payment.copy(sequenceNumber = paidCount + index + 1)
        }

        val (newPlanType, newInitialAmount, newInstallmentCount, newInstallmentAmount) = when (newStrategy) {
            is AmexInstallmentStrategy.FixedAmount -> {
                require(newStrategy.amount > 0.0) { "Importo rata deve essere maggiore di zero" }
                require(residual > 0.0) { "Residuo deve essere maggiore di zero" }
                val count = ceil(residual / newStrategy.amount).toInt()
                val regular = round2(newStrategy.amount)
                val amount = if (count == 1) round2(residual) else regular
                Quadruple("FIXED_AMOUNT", newStrategy.amount, paidCount + count, amount)
            }
            is AmexInstallmentStrategy.FixedDuration -> {
                require(newStrategy.months > 0) { "Durata deve essere maggiore di zero" }
                require(residual > 0.0) { "Residuo deve essere maggiore di zero" }
                val count = newStrategy.months
                val regular = round2(residual / count)
                val amount = if (count == 1) round2(residual) else regular
                Quadruple("FIXED_DURATION", null, paidCount + count, amount)
            }
        }

        val updatedPlan = existingPlan.copy(
            installmentCount = newInstallmentCount,
            installmentAmount = newInstallmentAmount,
            planType = newPlanType,
            initialInstallmentAmount = newInitialAmount,
        )

        return Pair(updatedPlan, newPendingPayments)
    }

    private fun round2(value: Double): Double = round(value * 100.0) / 100.0

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
    )
}
