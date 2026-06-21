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
        val paymentTransactions = dueNow.map { payment ->
            val plan = plans.find { it.id == payment.planId }
            TransactionEntity(
                id = UUID.randomUUID().toString(),
                date = paymentDate,
                description = "Rata Amex ${payment.sequenceNumber}/${plan?.installmentCount ?: "-"}",
                amount = payment.amount,
                categoryId = "credit_card_payment",
                type = TransactionType.EXPENSE,
                isCreditCard = false,
                originalAmount = payment.amount,
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
}
