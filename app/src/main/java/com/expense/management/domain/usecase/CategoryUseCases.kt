package com.expense.management.domain.usecase

import com.expense.management.data.CategoryEntity
import com.expense.management.data.ExpenseRepository
import com.expense.management.ui.screens.category.CATEGORIES
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetCategoriesUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(): Flow<List<CategoryEntity>> = repository.allCategoriesFlow.map { dbCategories ->
        val dbIds = dbCategories.map { it.id }.toSet()
        val missingDefaults = CATEGORIES.filter { it.id !in dbIds }.map {
            CategoryEntity(
                id = it.id,
                label = it.label,
                icon = it.icon,
                type = it.type,
                isCustom = false,
            )
        }
        dbCategories + missingDefaults
    }
}

class InitializeCategoriesUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke() {
        val existingCategories = repository.getAllCategories()
        val existingIds = existingCategories.map { it.id }.toSet()
        val categoriesToAdd = CATEGORIES.filter { it.id !in existingIds }.map {
            CategoryEntity(
                id = it.id,
                label = it.label,
                icon = it.icon,
                type = it.type,
                isCustom = false,
            )
        }
        if (categoriesToAdd.isNotEmpty()) {
            repository.insertAllCategories(categoriesToAdd)
        }
    }
}
