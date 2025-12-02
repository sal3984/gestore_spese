package com.expense.management.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class TransactionEntity(
    // ID ora è una stringa UUID generata in automatico
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String,
    val description: String,
    val amount: Double,
    val categoryId: String,
    // "expense" o "income"
    val type: String,
    val isCreditCard: Boolean,
    // Data reale di addebito
    val effectiveDate: String,
    // Importo nella valuta originale (es. 50 USD)
    val originalAmount: Double,
    // Valuta usata per la transazione (es. USD)
    val originalCurrency: String,
    // Campi per pagamento rateale  Es. 1 (di 3)
    val installmentNumber: Int? = null,
    val totalInstallments: Int? = null,
    // UUID condiviso tra tutte le rate della stessa spesa
    val groupId: String? = null,
)


