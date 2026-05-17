package com.expense.management.domain.usecase

import com.expense.management.data.CategoryEntity
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetFrequentCategoriesUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(type: TransactionType, limit: Int = 4): Flow<List<CategoryEntity>> {
        return combine(repository.allTransactions, repository.allCategoriesFlow) { transactions, categories ->
            val topCategoryIds = transactions
                .filter { it.type == type }
                .groupBy { it.categoryId }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .take(limit)
                .map { it.first }

            categories.filter { it.id in topCategoryIds && it.type == type }
        }
    }
}
