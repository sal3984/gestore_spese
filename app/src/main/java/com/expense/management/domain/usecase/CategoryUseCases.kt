package com.expense.management.domain.usecase

import com.expense.management.data.ExpenseRepository
import com.expense.management.data.toData
import com.expense.management.data.toDomain
import com.expense.management.domain.model.CATEGORIES
import com.expense.management.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetCategoriesUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(): Flow<List<Category>> = repository.allCategoriesFlow.map { dbCategories ->
        val dbIds = dbCategories.map { it.id }.toSet()
        val missingDefaults = CATEGORIES.filter { it.id !in dbIds }.map {
            Category(
                id = it.id,
                label = it.label,
                icon = it.icon,
                type = it.type,
                isCustom = false,
                imageUri = null,
            )
        }
        dbCategories.map { it.toDomain() } + missingDefaults
    }
}

class InitializeCategoriesUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke() {
        val existingCategories = repository.getAllCategories()
        val existingIds = existingCategories.map { it.id }.toSet()
        val categoriesToAdd = CATEGORIES.filter { it.id !in existingIds }.map {
            com.expense.management.data.CategoryEntity(
                id = it.id,
                label = it.label,
                icon = it.icon,
                type = it.type.toData(),
                isCustom = false,
                imageUri = null,
            )
        }
        if (categoriesToAdd.isNotEmpty()) {
            repository.insertAllCategories(categoriesToAdd)
        }
    }
}
