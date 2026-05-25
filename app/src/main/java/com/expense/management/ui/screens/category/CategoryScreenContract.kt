package com.expense.management.ui.screens.category

import com.expense.management.data.CategoryEntity
import com.expense.management.data.TransactionType

data class CategoryUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val selectedTab: TransactionType = TransactionType.EXPENSE,
    val showDialog: Boolean = false,
    val categoryToEdit: CategoryEntity? = null,
)

sealed interface CategoryEvent {
    data class OnTabChanged(val tab: TransactionType) : CategoryEvent
    data object OnAddCategoryClick : CategoryEvent
    data class OnEditCategoryClick(val category: CategoryEntity) : CategoryEvent
    data class OnDeleteCategoryClick(val categoryId: String) : CategoryEvent
    data object OnDialogDismiss : CategoryEvent
    data class OnDialogConfirm(val label: String, val icon: String, val imageUri: String?) : CategoryEvent
}
