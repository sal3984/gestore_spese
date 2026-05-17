package com.expense.management.domain.usecase

import com.expense.management.data.ExpenseRepository
import com.expense.management.data.TransactionEntity

class SaveTransactionUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(transaction: TransactionEntity) {
        val existingTransaction = repository.getTransactionById(transaction.id)

        if (existingTransaction != null && transaction.groupId != null && (transaction.totalInstallments ?: 1) > 1) {
            if (existingTransaction.categoryId != transaction.categoryId) {
                val transactionsInGroup = repository.getAllTransactionsList()
                    .filter { it.groupId == transaction.groupId }

                transactionsInGroup.forEach { installment ->
                    repository.insertTransaction(installment.copy(categoryId = transaction.categoryId))
                }
            } else {
                repository.insertTransaction(transaction)
            }
        } else {
            repository.insertTransaction(transaction)
        }
    }
}
