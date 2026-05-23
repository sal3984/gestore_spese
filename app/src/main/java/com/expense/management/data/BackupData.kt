package com.expense.management.data

data class BackupData(
    val transactions: List<TransactionEntity>?,
    val categories: List<CategoryEntity>?,
    val creditCard: List<CreditCardEntity>?,
)
