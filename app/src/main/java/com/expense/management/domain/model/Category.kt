package com.expense.management.domain.model

data class Category(
    val id: String,
    val label: String,
    val icon: String,
    val type: TransactionType,
    val isCustom: Boolean = false,
    val imageUri: String? = null,
)
