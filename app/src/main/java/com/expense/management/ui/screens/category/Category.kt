package com.expense.management.ui.screens.category

import com.expense.management.data.TransactionType

data class Category(
    val id: String,
    val label: String,
    val icon: String,
    val type: TransactionType,
)

val CATEGORIES =
    listOf(
        // USCITE (expense)
        Category("food", "Cibo", "\uD83C\uDF7D", TransactionType.EXPENSE),
        Category("transport", "Trasporti", "\uD83D\uDE97", TransactionType.EXPENSE),
        Category("housing", "Casa", "\uD83C\uDFE0", TransactionType.EXPENSE),
        Category("entertainment", "Svago", "\uD83C\uDFC1", TransactionType.EXPENSE),
        Category("bills", "Bollette", "\uD83D\uDCCB", TransactionType.EXPENSE),
        Category("health", "Salute", "\u2764\uFE0F", TransactionType.EXPENSE),
        Category("shopping", "Shopping", "\uD83D\uDED2", TransactionType.EXPENSE),
        Category("other", "Altro", "\u2753", TransactionType.EXPENSE),
        // ENTRATE (income)
        Category("salary", "Stipendio", "\uD83D\uDCB0", TransactionType.INCOME),
        Category("bonifico", "Bonifico", "\uD83D\uDCB3", TransactionType.INCOME),
        Category("gift", "Regalo", "\uD83C\uDF81", TransactionType.INCOME),
        Category("refund", "Rimborso", "\u21A9\uFE0F", TransactionType.INCOME),
        Category("investment", "Investimenti", "\uD83D\uDCCA", TransactionType.INCOME),
    )
