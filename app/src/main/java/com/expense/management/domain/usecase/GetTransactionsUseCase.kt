package com.expense.management.domain.usecase

import com.expense.management.data.ExpenseRepository
import com.expense.management.data.TransactionEntity
import kotlinx.coroutines.flow.Flow

class GetTransactionsUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(): Flow<List<TransactionEntity>> = repository.allTransactions
}
