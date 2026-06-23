package com.expense.management.domain.usecase

import com.expense.management.data.ExpenseRepository
import com.expense.management.data.toDomain
import com.expense.management.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetTransactionsUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(): Flow<List<Transaction>> = repository.allTransactions.map { list ->
        list.map { it.toDomain() }
    }
}
