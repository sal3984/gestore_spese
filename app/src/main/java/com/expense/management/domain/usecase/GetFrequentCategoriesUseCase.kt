package com.expense.management.domain.usecase

import com.expense.management.data.ExpenseRepository
import com.expense.management.data.toData
import com.expense.management.data.toDomain
import com.expense.management.domain.model.Category
import com.expense.management.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetFrequentCategoriesUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(type: TransactionType, limit: Int = 4): Flow<List<Category>> {
        return combine(
            repository.getTopCategoryIds(type.toData(), limit),
            repository.allCategoriesFlow,
        ) { topIds, categories ->
            val idSet = topIds.toSet()
            categories.filter { it.id in idSet && it.type.toDomain() == type }
                .map { it.toDomain() }
                .sortedBy { topIds.indexOf(it.id) }
        }
    }
}
