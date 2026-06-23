package com.expense.management.domain.usecase

import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity
import com.expense.management.data.AmexRevolvingStateEntity
import com.expense.management.data.AmexStatementEntity
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.domain.model.AmexCardHubData
import com.expense.management.domain.model.AmexDashboardProjection
import com.expense.management.domain.model.AmexHubData
import com.expense.management.domain.model.AmexStatementWithDetails
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class GetAmexHubDataUseCase(
    private val calculateStatementUseCase: CalculateAmexStatementUseCase = CalculateAmexStatementUseCase(),
    private val calculateOutflowUseCase: CalculateAmexCurrentAccountOutflowUseCase = CalculateAmexCurrentAccountOutflowUseCase(),
) {
    fun execute(
        amexPaymentMethods: List<PaymentMethodEntity>,
        allStatements: List<AmexStatementEntity>,
        allPagoFlexPlans: List<AmexPagoFlexPlanEntity>,
        allRevolvingStates: List<AmexRevolvingStateEntity>,
        allScheduledPayments: List<AmexPagoFlexScheduledPaymentEntity>,
        projections: List<AmexDashboardProjection>,
        isAutoPayEnabled: Boolean,
        targetMonth: YearMonth,
    ): AmexHubData {
        val pagoFlexByStatement = allPagoFlexPlans.groupBy { it.statementId }
        val revolvingByStatement = allRevolvingStates.associateBy { it.statementId }
        val targetMonthStr = targetMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val projectionByMethod = projections.associateBy { it.paymentMethodId }
        val planIds: Set<String> = allPagoFlexPlans.map { it.id }.toSet()
        val scheduledByPlan = allScheduledPayments
            .filter { it.planId in planIds }
            .groupBy { it.planId }

        val cards = amexPaymentMethods.map { method ->
            val cardStatements = allStatements
                .filter { it.paymentMethodId == method.id }
                .sortedByDescending { it.statementMonth }

            val statementsWithDetails = cardStatements.map { statement ->
                val plans = pagoFlexByStatement[statement.id].orEmpty()
                val revolving = revolvingByStatement[statement.id]
                val summary = calculateStatementUseCase.execute(statement, plans, revolving)
                AmexStatementWithDetails(
                    statement = statement,
                    summary = summary,
                    pagoFlexPlans = plans,
                    revolvingState = revolving,
                )
            }

            val cardPlanIds = statementsWithDetails
                .flatMap { it.pagoFlexPlans }
                .map { it.id }
                .toSet()
            val cardScheduledPayments = cardPlanIds
                .flatMap { pid -> scheduledByPlan[pid].orEmpty() }
                .filter { it.dueDate.startsWith(targetMonthStr) }

            val outflow = calculateOutflowUseCase.execute(targetMonthStr, cardScheduledPayments)

            AmexCardHubData(
                cardName = method.name,
                paymentMethodId = method.id,
                statements = statementsWithDetails,
                projection = projectionByMethod[method.id],
                scheduledPayments = cardScheduledPayments,
                currentAccountOutflow = outflow,
            )
        }

        return AmexHubData(
            autoPayEnabled = isAutoPayEnabled,
            cards = cards,
        )
    }
}
