package com.alessandrogregorio.gestorespese.data

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
    val type: String, // "expense" o "income"
    val isCreditCard: Boolean,
    val effectiveDate: String, // Data reale di addebito
    val originalAmount: Double, // Importo nella valuta originale (es. 50 USD)
    val originalCurrency: String, // Valuta usata per la transazione (es. USD)
    // Campi per pagamento rateale
    val installmentNumber: Int? = null, // Es. 1 (di 3)
    val totalInstallments: Int? = null, // Es. 3
    val groupId: String? = null // UUID condiviso tra tutte le rate della stessa spesa
)
