package com.expense.management.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["effectiveDate"]),
        Index(value = ["categoryId"]),
        Index(value = ["groupId"]),
        Index(value = ["creditCardId"]),
        Index(value = ["paymentMethodId"]),
    ],
)
data class TransactionEntity(
    // ID ora è una stringa UUID generata in automatico
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String,
    val description: String,
    val amount: Double,
    val categoryId: String,
    // "expense" o "income"
    val type: TransactionType,
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
    // UUID condiviso tra tutte le rate o transazioni ricorrenti dello stesso gruppo
    val groupId: String? = null,
    // ID della carta di credito usata (opzionale, legacy)
    val creditCardId: String? = null,
    // ID del metodo di pagamento usato (nuovo sistema multifornitore)
    val paymentMethodId: String? = null,
    // Campi per transazioni ricorrenti
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceEndDate: String? = null,
    val recurrenceLimit: Int? = null,
)
