package com.expense.management.domain.usecase

import com.expense.management.data.ExpenseRepository
import com.expense.management.ui.screens.DeleteType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DeleteTransactionUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(transactionId: String, deleteType: DeleteType) {
        val transactionToDelete = repository.getTransactionById(transactionId) ?: return

        when (deleteType) {
            DeleteType.SINGLE -> repository.deleteTransaction(transactionId)
            DeleteType.THIS_AND_SUBSEQUENT -> {
                val groupId = transactionToDelete.groupId
                if (groupId != null) {
                    val transactionsInGroup = repository.getAllTransactionsList()
                        .filter { it.groupId == groupId }
                        .filter {
                            try {
                                LocalDate.parse(it.effectiveDate, DateTimeFormatter.ISO_LOCAL_DATE) >=
                                    LocalDate.parse(transactionToDelete.effectiveDate, DateTimeFormatter.ISO_LOCAL_DATE)
                            } catch (e: Exception) {
                                false
                            }
                        }

                    transactionsInGroup.forEach { installment ->
                        repository.deleteTransaction(installment.id)
                    }
                } else {
                    repository.deleteTransaction(transactionId)
                }
            }
        }
    }
}
