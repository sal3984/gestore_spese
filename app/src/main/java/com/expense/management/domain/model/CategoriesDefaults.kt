package com.expense.management.domain.model

data class DefaultCategory(
    val id: String,
    val label: String,
    val icon: String,
    val type: TransactionType,
) {
    /** Nome nella lingua del telefono, o quello storico se non tradotto. */
    fun labelIn(names: DefaultCategoryNames): String = names.nameOf(id) ?: label
}

/**
 * Nomi delle categorie predefinite nella lingua del telefono: si usano quando la categoria viene
 * creata, poi restano quelli salvati (chi ha gia' l'app tiene i suoi).
 */
fun interface DefaultCategoryNames {
    fun nameOf(categoryId: String): String?

    companion object {
        /** Nomi storici in italiano: ripiego per i test. */
        val Default = DefaultCategoryNames { null }
    }
}

val CATEGORIES =
    listOf(
        DefaultCategory("food", "Cibo", "\uD83C\uDF7D", TransactionType.EXPENSE),
        DefaultCategory("transport", "Trasporti", "\uD83D\uDE97", TransactionType.EXPENSE),
        DefaultCategory("housing", "Casa", "\uD83C\uDFE0", TransactionType.EXPENSE),
        DefaultCategory("entertainment", "Svago", "\uD83C\uDFC1", TransactionType.EXPENSE),
        DefaultCategory("bills", "Bollette", "\uD83D\uDCCB", TransactionType.EXPENSE),
        DefaultCategory("health", "Salute", "\u2764\uFE0F", TransactionType.EXPENSE),
        DefaultCategory("shopping", "Shopping", "\uD83D\uDED2", TransactionType.EXPENSE),
        DefaultCategory("other", "Altro", "\u2753", TransactionType.EXPENSE),
        DefaultCategory("credit_card_payment", "Addebito Carta", "\uD83D\uDCB3", TransactionType.EXPENSE),
        DefaultCategory("salary", "Stipendio", "\uD83D\uDCB0", TransactionType.INCOME),
        DefaultCategory("bonifico", "Bonifico", "\uD83D\uDCB3", TransactionType.INCOME),
        DefaultCategory("gift", "Regalo", "\uD83C\uDF81", TransactionType.INCOME),
        DefaultCategory("refund", "Rimborso", "\u21A9\uFE0F", TransactionType.INCOME),
        DefaultCategory("investment", "Investimenti", "\uD83D\uDCCA", TransactionType.INCOME),
        DefaultCategory("credit_card_adjustment", "Credito Carta", "\uD83D\uDCB3", TransactionType.INCOME),
    )
