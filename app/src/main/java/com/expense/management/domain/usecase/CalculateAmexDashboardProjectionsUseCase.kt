package com.expense.management.domain.usecase

import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexRevolvingStateEntity
import com.expense.management.data.AmexStatementEntity
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.domain.model.AmexDashboardProjection
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CalculateAmexDashboardProjectionsUseCase {

    fun execute(
        targetMonth: YearMonth,
        allPaymentMethods: List<PaymentMethodEntity>,
        allStatements: List<AmexStatementEntity>,
        allPagoFlexPlans: List<AmexPagoFlexPlanEntity>,
        allRevolvingStates: List<AmexRevolvingStateEntity>,
    ): List<AmexDashboardProjection> {
        val amexMethods = allPaymentMethods.filter { it.provider.name == "CREDIT_CARD_AMEX" }
        val plansByStatement = allPagoFlexPlans.groupBy { it.statementId }
        val revolvingByStatement = allRevolvingStates.associateBy { it.statementId }
        val targetStr = targetMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val today = LocalDate.now()

        return amexMethods.map { method ->
            val cardStatements = allStatements
                .filter { it.paymentMethodId == method.id }
                .sortedByDescending { it.statementMonth }
            val openStatement = cardStatements.firstOrNull { !it.isClosed }
            val hasOpenStatement = openStatement != null

            val pagoflexQuotaTotal = plansByStatement.flatMap { (_, plans) ->
                plans.filter { plan ->
                    val startMonth = try {
                        YearMonth.parse(plan.startDate.substring(0, 7))
                    } catch (_: Exception) {
                        null
                    }
                    startMonth != null && startMonth <= targetMonth
                }
            }.sumOf { plan ->
                val startMonth = try {
                    YearMonth.parse(plan.startDate.substring(0, 7))
                } catch (_: Exception) {
                    null
                }
                if (startMonth != null) {
                    val monthsElapsed = YearMonth.from(targetMonth.atDay(1)).let { tm ->
                        (tm.year - startMonth.year) * 12 + (tm.monthValue - startMonth.monthValue)
                    }
                    val effectivePaid = monthsElapsed + 1
                    if (effectivePaid <= plan.installmentCount && effectivePaid > 0) {
                        plan.installmentAmount
                    } else {
                        0.0
                    }
                } else {
                    0.0
                }
            }
            val pagoflexPlanCount = plansByStatement.flatMap { (_, plans) -> plans }
                .count { plan ->
                    val startMonth = try {
                        YearMonth.parse(plan.startDate.substring(0, 7))
                    } catch (_: Exception) {
                        null
                    }
                    startMonth != null && !startMonth.isAfter(targetMonth)
                }

            val dueAmount = if (openStatement != null) {
                val dueDate = try {
                    LocalDate.parse(openStatement.paymentDueDate, DateTimeFormatter.ISO_LOCAL_DATE)
                } catch (_: Exception) {
                    null
                }
                if (dueDate != null && dueDate.month == targetMonth.month && dueDate.year == targetMonth.year) {
                    openStatement.paymentAmount
                } else {
                    0.0
                }
            } else {
                0.0
            }

            val hasDuePayment = dueAmount > 0.0 && openStatement != null && !openStatement.isClosed

            AmexDashboardProjection(
                paymentMethodId = method.id,
                cardName = method.name,
                targetMonth = targetStr,
                pagoflexQuotaTotal = pagoflexQuotaTotal,
                pagoflexPlanCount = pagoflexPlanCount,
                hasDuePayment = hasDuePayment,
                duePaymentAmount = dueAmount,
                hasOpenStatement = hasOpenStatement,
            )
        }.filter { it.pagoflexQuotaTotal > 0.0 || it.hasDuePayment || it.hasOpenStatement }
    }
}
