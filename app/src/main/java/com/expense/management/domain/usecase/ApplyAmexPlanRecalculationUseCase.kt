package com.expense.management.domain.usecase

import com.expense.management.data.ExpenseRepository
import com.expense.management.domain.model.AmexInstallmentStrategy
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ApplyAmexPlanRecalculationUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(
        planId: String,
        strategy: AmexInstallmentStrategy,
        currentStatementPaymentDueDate: String,
    ) {
        repository.withTransaction {
            val plan = repository.getAmexPagoFlexPlanById(planId) ?: return@withTransaction
            val existingPayments = repository.getAmexScheduledPaymentsForPlan(planId)
            val syncedPending = existingPayments
                .filter { it.status == "PENDING" && it.expenseTransactionId != null }
            val syncedByMonth = syncedPending.associate { it.dueDate.take(7) to it.expenseTransactionId!! }
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val (updatedPlan, newPendingPayments) = RecalculateAmexInstallmentPlanUseCase().execute(
                existingPlan = plan,
                existingPayments = existingPayments,
                newStrategy = strategy,
                today = today,
                currentStatementPaymentDueDate = currentStatementPaymentDueDate,
            )
            repository.updateAmexPagoFlexPlanCalculation(
                planId = updatedPlan.id,
                installmentCount = updatedPlan.installmentCount,
                installmentAmount = updatedPlan.installmentAmount,
                planType = updatedPlan.planType,
                initialInstallmentAmount = updatedPlan.initialInstallmentAmount,
            )
            repository.deletePendingAmexScheduledPaymentsForPlan(planId)
            repository.insertAmexScheduledPayments(newPendingPayments)

            for (newPayment in newPendingPayments) {
                val monthKey = newPayment.dueDate.take(7)
                val oldTxId = syncedByMonth[monthKey] ?: continue
                val tx = repository.getTransactionById(oldTxId) ?: continue
                repository.updateAmexScheduledPaymentExpenseTransactionId(newPayment.id, oldTxId)
                repository.insertTransaction(
                    tx.copy(
                        amount = newPayment.amount,
                        originalAmount = newPayment.amount,
                        effectiveDate = newPayment.dueDate,
                    ),
                )
            }
        }
    }
}
