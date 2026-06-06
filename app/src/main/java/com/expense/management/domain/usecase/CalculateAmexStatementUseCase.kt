package com.expense.management.domain.usecase

import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexRevolvingStateEntity
import com.expense.management.data.AmexStatementEntity
import com.expense.management.domain.model.AmexPaymentMode
import com.expense.management.domain.model.AmexStatementSummary

class CalculateAmexStatementUseCase {

    fun execute(
        statement: AmexStatementEntity,
        pagoFlexPlans: List<AmexPagoFlexPlanEntity>,
        revolvingState: AmexRevolvingStateEntity?,
        interestRate: Double = 0.0,
    ): AmexStatementSummary {
        val totalExpenses = statement.totalExpenses
        val totalPagoflex = statement.totalPagoflex
        val revolvingEligible = totalExpenses - totalPagoflex
        val pagoflexQuota = pagoFlexPlans.sumOf {
            if (it.paidCount < it.installmentCount) it.installmentAmount else 0.0
        }
        val paymentMode = AmexPaymentMode.safeValueOf(statement.paymentMode) ?: AmexPaymentMode.SALDO
        val carriedForward = revolvingState?.carriedForwardDebt ?: statement.revolvingBalance

        return AmexStatementSummary(
            statementId = statement.id,
            paymentMethodId = statement.paymentMethodId,
            statementMonth = statement.statementMonth,
            totalExpenses = totalExpenses,
            totalPagoflex = totalPagoflex,
            revolvingEligible = revolvingEligible,
            pagoflexQuota = pagoflexQuota,
            paymentMode = paymentMode,
            paymentAmount = statement.paymentAmount,
            carriedForward = carriedForward,
            interestRate = interestRate,
        )
    }
}
