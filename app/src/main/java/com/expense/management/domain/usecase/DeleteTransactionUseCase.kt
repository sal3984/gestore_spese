package com.expense.management.domain.usecase

import com.expense.management.data.ExpenseRepository
import com.expense.management.data.RecurrenceType
import com.expense.management.domain.model.DeleteType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DeleteTransactionUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(transactionId: String, deleteType: DeleteType) {
        val transactionToDelete = repository.getTransactionById(transactionId) ?: return

        when (deleteType) {
            DeleteType.SINGLE -> {
                repository.deleteTransaction(transactionId)
                revertLinkedScheduledPayments(transactionId)
            }
            DeleteType.THIS_AND_SUBSEQUENT -> {
                val groupId = transactionToDelete.groupId
                if (groupId != null && transactionToDelete.recurrenceType != RecurrenceType.NONE) {
                    val transactionsInGroup = repository.getTransactionsByGroupId(groupId)
                        .filter {
                            try {
                                LocalDate.parse(it.effectiveDate, DateTimeFormatter.ISO_LOCAL_DATE) >=
                                    LocalDate.parse(transactionToDelete.effectiveDate, DateTimeFormatter.ISO_LOCAL_DATE)
                            } catch (e: Exception) {
                                false
                            }
                        }

                    val idsToDelete = transactionsInGroup.map { it.id }
                    repository.deleteTransactionsByIds(idsToDelete)
                    idsToDelete.forEach { revertLinkedScheduledPayments(it) }
                } else {
                    repository.deleteTransaction(transactionId)
                    revertLinkedScheduledPayments(transactionId)
                }
            }
        }
    }

    private suspend fun revertLinkedScheduledPayments(transactionId: String) {
        val amexPayment = repository.getAmexScheduledPaymentByExpenseTxId(transactionId)
        if (amexPayment != null) {
            repository.revertAmexScheduledPaymentToPending(amexPayment.id)
            val plan = repository.getAmexPagoFlexPlanById(amexPayment.planId)
            if (plan != null) {
                val paidCount = repository.getAmexScheduledPaymentsForPlan(plan.id).count { it.status == "PAID" }
                repository.updateAmexPagoFlexPaidCount(plan.id, paidCount)
            }
        }
        val genericPayment = repository.getGenericScheduledPaymentByExpenseTxId(transactionId)
        if (genericPayment != null) {
            repository.revertGenericScheduledPaymentToPending(genericPayment.id)
            val paidCount = repository.getScheduledPaymentsByPlan(genericPayment.planId).count { it.status == "PAID" }
            repository.updateInstallmentPlanPaidCount(genericPayment.planId, paidCount)
        }
    }
}
