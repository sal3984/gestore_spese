package com.expense.management.domain.model

data class ReceiptScanResult(
    val amount: Double? = null,
    val description: String? = null,
    val date: String? = null,
    val rawText: String = "",
)
