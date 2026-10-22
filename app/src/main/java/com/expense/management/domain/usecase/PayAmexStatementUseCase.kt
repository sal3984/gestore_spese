package com.expense.management.domain.usecase

import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity
import com.expense.management.data.AmexStatementEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import java.util.UUID

class PayAmexStatementUseCase {

    fun execute(
        statement: AmexStatementEntity,
        amount: Double,
        paymentDate: String,
        plans: List<AmexPagoFlexPlanEntity>,
        scheduledPayments: List<AmexPagoFlexScheduledPaymentEntity>,
        linkedPaymentMethodId: String? = null,
    ): AmexPaymentResult {
        val hasInstallmentPlan = plans.isNotEmpty()
        if (!hasInstallmentPlan) {
            val paymentTransaction = TransactionEntity(
                id = UUID.randomUUID().toString(),
                date = paymentDate,
                description = "Pagamento Amex ${statement.statementMonth}",
                amount = amount,
                categoryId = "credit_card_payment",
                type = TransactionType.EXPENSE,
                isCreditCard = false,
                originalAmount = amount,
                originalCurrency = "€",
                effectiveDate = paymentDate,
                paymentMethodId = linkedPaymentMethodId,
            )
            val incomeTransaction = TransactionEntity(
                id = UUID.randomUUID().toString(),
                date = paymentDate,
                description = "Rimborso Amex ${statement.statementMonth}",
                amount = amount,
                categoryId = "credit_card_adjustment",
                type = TransactionType.INCOME,
                isCreditCard = true,
                originalAmount = amount,
                originalCurrency = "€",
                effectiveDate = paymentDate,
                creditCardId = statement.paymentMethodId,
                paymentMethodId = statement.paymentMethodId,
            )
            return AmexPaymentResult(
                paymentTransactions = listOf(paymentTransaction),
                incomeTransaction = incomeTransaction,
                paidInstallments = emptyList(),
            )
        }
        val planIds = plans.map { it.id }.toSet()
        val pendingByPlan = scheduledPayments
            .filter { it.planId in planIds && it.status == "PENDING" && it.expenseTransactionId == null }
            .groupBy { it.planId }
        val dueNow = pendingByPlan.values
            .mapNotNull { payments -> payments.minByOrNull { it.sequenceNumber } }
        val splitAmounts = splitProportionally(amount, dueNow.map { it.amount })
        val paymentTransactions = dueNow.mapIndexed { index, payment ->
            val plan = plans.find { it.id == payment.planId }
            val txAmount = splitAmounts[index]
            TransactionEntity(
                id = UUID.randomUUID().toString(),
                date = paymentDate,
                description = "Rata Amex ${payment.sequenceNumber}/${plan?.installmentCount ?: "-"}",
                amount = txAmount,
                categoryId = "credit_card_payment",
                type = TransactionType.EXPENSE,
                isCreditCard = false,
                originalAmount = txAmount,
                originalCurrency = "€",
                effectiveDate = paymentDate,
                installmentNumber = payment.sequenceNumber,
                totalInstallments = plan?.installmentCount,
                groupId = payment.planId,
                paymentMethodId = linkedPaymentMethodId,
            )
        }
        return AmexPaymentResult(
            paymentTransactions = paymentTransactions,
            incomeTransaction = null,
            paidInstallments = dueNow,
        )
    }

    data class AmexPaymentResult(
        val paymentTransactions: List<TransactionEntity>,
        val incomeTransaction: TransactionEntity?,
        val paidInstallments: List<AmexPagoFlexScheduledPaymentEntity>,
    )

    private fun splitProportionally(total: Double, weights: List<Double>): List<Double> {
        if (weights.isEmpty()) return emptyList()
        val weightSum = weights.sum()
        val shares = if (weightSum > 0.0) {
            weights.map { total * it / weightSum }
        } else {
            weights.map { total / weights.size }
        }
        val allButLast = shares.dropLast(1).map { round2(it) }
        return allButLast + round2(total - allButLast.sum())
    }

    private fun round2(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0
}
