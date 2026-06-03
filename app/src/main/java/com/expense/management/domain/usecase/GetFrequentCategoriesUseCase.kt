package com.expense.management.domain.usecase

import com.expense.management.data.CategoryEntity
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetFrequentCategoriesUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(type: TransactionType, limit: Int = 4): Flow<List<CategoryEntity>> {
        return combine(repository.getTopCategoryIds(type, limit), repository.allCategoriesFlow) { topIds, categories ->
            val idSet = topIds.toSet()
            categories.filter { it.id in idSet && it.type == type }
                .sortedBy { topIds.indexOf(it.id) }
        }
    }
}
