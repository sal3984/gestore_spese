package com.expense.management.data.mapper

fun com.expense.management.domain.model.TransactionType.toData(): com.expense.management.data.TransactionType = when (this) {
    com.expense.management.domain.model.TransactionType.INCOME -> com.expense.management.data.TransactionType.INCOME
    com.expense.management.domain.model.TransactionType.EXPENSE -> com.expense.management.data.TransactionType.EXPENSE
}

fun com.expense.management.data.TransactionType.toDomain(): com.expense.management.domain.model.TransactionType = when (this) {
    com.expense.management.data.TransactionType.INCOME -> com.expense.management.domain.model.TransactionType.INCOME
    com.expense.management.data.TransactionType.EXPENSE -> com.expense.management.domain.model.TransactionType.EXPENSE
}
