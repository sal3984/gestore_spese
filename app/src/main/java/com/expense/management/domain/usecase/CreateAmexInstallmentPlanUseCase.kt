package com.expense.management.domain.usecase

import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity
import com.expense.management.domain.model.AmexInstallmentStrategy
import kotlin.math.ceil
import kotlin.math.round

class CreateAmexInstallmentPlanUseCase(
    private val buildPaymentsUseCase: BuildAmexScheduledPaymentsUseCase = BuildAmexScheduledPaymentsUseCase(),
) {

    fun execute(
        planId: String,
        statementId: String,
        transactionId: String,
        totalAmount: Double,
        startDate: String,
        strategy: AmexInstallmentStrategy,
    ): Pair<AmexPagoFlexPlanEntity, List<AmexPagoFlexScheduledPaymentEntity>> {
        require(totalAmount > 0.0) { "Importo totale deve essere maggiore di zero" }
        val (planType, initialInstallmentAmount, installmentCount, installmentAmount) = when (strategy) {
            is AmexInstallmentStrategy.FixedAmount -> {
                require(strategy.amount > 0.0) { "Importo rata deve essere maggiore di zero" }
                val count = ceil(totalAmount / strategy.amount).toInt()
                val regular = round2(strategy.amount)
                val amount = if (count == 1) round2(totalAmount) else regular
                Quadruple("FIXED_AMOUNT", strategy.amount, count, amount)
            }
            is AmexInstallmentStrategy.FixedDuration -> {
                require(strategy.months > 0) { "Durata deve essere maggiore di zero" }
                val count = strategy.months
                val regular = round2(totalAmount / count)
                val amount = if (count == 1) round2(totalAmount) else regular
                Quadruple("FIXED_DURATION", null, count, amount)
            }
        }

        val plan = AmexPagoFlexPlanEntity(
            id = planId,
            statementId = statementId,
            transactionId = transactionId,
            totalAmount = totalAmount,
            installmentCount = installmentCount,
            installmentAmount = installmentAmount,
            paidCount = 0,
            startDate = startDate,
            planType = planType,
            initialInstallmentAmount = initialInstallmentAmount,
        )

        val payments = buildPaymentsUseCase.execute(
            planId = planId,
            totalAmount = totalAmount,
            strategy = strategy,
            startDate = startDate,
        )

        return Pair(plan, payments)
    }

    private fun round2(value: Double): Double = round(value * 100.0) / 100.0

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
    )
}
