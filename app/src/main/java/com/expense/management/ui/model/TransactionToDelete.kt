package com.expense.management.ui.model

import com.expense.management.data.TransactionEntity

data class TransactionToDelete(
    val transaction: TransactionEntity,
    val isInstallment: Boolean,
    val isAmexInstallment: Boolean = false,
)
