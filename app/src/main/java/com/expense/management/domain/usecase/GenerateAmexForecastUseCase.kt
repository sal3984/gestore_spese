package com.expense.management.domain.usecase

import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexRevolvingStateEntity
import com.expense.management.domain.model.AmexForecastMonth
import com.expense.management.domain.model.AmexPaymentMode
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class GenerateAmexForecastUseCase {

    fun execute(
        statementMonth: String,
        pagoFlexPlans: List<AmexPagoFlexPlanEntity>,
        revolvingState: AmexRevolvingStateEntity?,
        paymentMode: AmexPaymentMode,
        paymentAmount: Double,
        interestRate: Double,
        monthsToForecast: Int = 3,
    ): List<AmexForecastMonth> {
        val baseMonth = try {
            YearMonth.parse(statementMonth)
        } catch (_: Exception) {
            YearMonth.now()
        }
        var carriedForward = revolvingState?.carriedForwardDebt ?: 0.0
        val monthlyRate = interestRate / 100.0 / 12.0
        val forecast = mutableListOf<AmexForecastMonth>()

        for (i in 1..monthsToForecast) {
            val forecastMonth = baseMonth.plusMonths(i.toLong())
            val monthLabel = forecastMonth.format(DateTimeFormatter.ofPattern("MMM yyyy"))

            val pagoflexQuota = pagoFlexPlans.sumOf { plan ->
                val monthsElapsed = i
                val effectivePaid = plan.paidCount + monthsElapsed
                if (effectivePaid <= plan.installmentCount && effectivePaid > plan.paidCount) {
                    if (effectivePaid == plan.installmentCount) {
                        val regularTotal = plan.installmentAmount * (plan.installmentCount - 1)
                        plan.totalAmount - regularTotal
                    } else {
                        plan.installmentAmount
                    }
                } else {
                    0.0
                }
            }

            val interest = if (carriedForward > 0 && monthlyRate > 0) {
                carriedForward * monthlyRate
            } else {
                0.0
            }

            val revolvingPayment = when (paymentMode) {
                AmexPaymentMode.SALDO -> {
                    if (i == 1) carriedForward else 0.0
                }
                AmexPaymentMode.FIXED -> {
                    if (carriedForward > 0) paymentAmount.coerceAtMost(carriedForward) else 0.0
                }
                AmexPaymentMode.MINIMUM -> {
                    val minPayment = carriedForward * 0.05
                    if (carriedForward > 0) minPayment.coerceAtMost(carriedForward) else 0.0
                }
                AmexPaymentMode.PAGOFLEX_ONLY -> 0.0
            }

            val totalDue = pagoflexQuota + revolvingPayment + interest
            carriedForward = carriedForward - revolvingPayment + interest
            if (carriedForward < 0.01) carriedForward = 0.0

            forecast.add(
                AmexForecastMonth(
                    monthOffset = i,
                    monthLabel = monthLabel,
                    pagoflexQuota = pagoflexQuota,
                    revolvingPayment = revolvingPayment,
                    interest = interest,
                    carriedForwardAfter = carriedForward,
                    totalDue = totalDue,
                ),
            )
        }

        return forecast
    }
}
