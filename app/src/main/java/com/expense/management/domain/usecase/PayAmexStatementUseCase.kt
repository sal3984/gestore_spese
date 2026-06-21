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
    ): AmexPaymentResult {
        val hasInstallmentPlan = plans.isNotEmpty()
        val paymentTransaction = if (!hasInstallmentPlan) {
            TransactionEntity(
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
            )
        } else {
            null
        }
        val incomeTransaction = if (!hasInstallmentPlan) {
            TransactionEntity(
                id = UUID.randomUUID().toString(),
                date = paymentDate,
                description = "Rimborso Amex ${statement.statementMonth}",
                amount = amount,
                categoryId = "credit_card_adjustment",
                type = TransactionType.INCOME,
                isCreditCard = false,
                originalAmount = amount,
                originalCurrency = "€",
                effectiveDate = paymentDate,
            )
        } else {
            null
        }
        val paymentsToMarkPaid = if (hasInstallmentPlan) {
            val planIds = plans.map { it.id }.toSet()
            scheduledPayments.filter { it.planId in planIds && it.status == "PENDING" }
        } else {
            emptyList()
        }
        return AmexPaymentResult(
            paymentTransaction = paymentTransaction,
            incomeTransaction = incomeTransaction,
            paymentsToMarkPaid = paymentsToMarkPaid,
        )
    }

    data class AmexPaymentResult(
        val paymentTransaction: TransactionEntity?,
        val incomeTransaction: TransactionEntity?,
        val paymentsToMarkPaid: List<AmexPagoFlexScheduledPaymentEntity>,
    )
}
