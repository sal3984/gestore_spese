package com.expense.management.domain.model

data class Transaction(
    val id: String,
    val date: String,
    val description: String,
    val amount: Double,
    val categoryId: String,
    val type: TransactionType,
    val isCreditCard: Boolean,
    val effectiveDate: String,
    val originalAmount: Double,
    val originalCurrency: String,
    val installmentNumber: Int? = null,
    val totalInstallments: Int? = null,
    val groupId: String? = null,
    val creditCardId: String? = null,
    val paymentMethodId: String? = null,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceEndDate: String? = null,
    val recurrenceLimit: Int? = null,
)
