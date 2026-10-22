package com.expense.management.domain.usecase

import com.expense.management.data.ExpenseRepository
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class MaterializeDueAmexInstallmentsUseCase(private val repository: ExpenseRepository) {

    suspend operator fun invoke(today: LocalDate = LocalDate.now()): Int {
        val payments = repository.getPendingAmexScheduledPaymentsDueBy(
            today.format(DateTimeFormatter.ISO_LOCAL_DATE),
        )
        if (payments.isEmpty()) return 0

        val plans = repository.getAllAmexPagoFlexPlans()
        val creditCardDetails = repository.getAllCreditCardDetails()
        val touchedPlans = mutableSetOf<String>()
        var materialized = 0

        for (payment in payments) {
            val plan = plans.find { it.id == payment.planId } ?: continue
            val statement = repository.getAmexStatementById(plan.statementId) ?: continue
            val detail = creditCardDetails.find { it.paymentMethodId == statement.paymentMethodId }
            val transaction = TransactionEntity(
                id = UUID.randomUUID().toString(),
                date = payment.dueDate,
                description = "Rata Amex ${payment.sequenceNumber}/${plan.installmentCount}",
                amount = payment.amount,
                categoryId = "credit_card_payment",
                type = TransactionType.EXPENSE,
                isCreditCard = false,
                originalAmount = payment.amount,
                originalCurrency = "\u20ac",
                effectiveDate = payment.dueDate,
                installmentNumber = payment.sequenceNumber,
                totalInstallments = plan.installmentCount,
                groupId = plan.id,
                paymentMethodId = detail?.linkedPaymentMethodId,
            )
            repository.withTransaction {
                repository.insertTransaction(transaction)
                repository.markAmexScheduledPaymentAsPaid(payment.id, transaction.id)
            }
            touchedPlans.add(plan.id)
            materialized++
        }

        for (planId in touchedPlans) {
            val paidCount = repository.getAmexScheduledPaymentsForPlan(planId).count { it.status == "PAID" }
            repository.updateAmexPagoFlexPaidCount(planId, paidCount)
        }
        return materialized
    }
}
