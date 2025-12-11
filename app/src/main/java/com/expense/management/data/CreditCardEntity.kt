package com.expense.management.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val limit: Double,
    // Giorno di chiusura estratto conto (es. 27). 0 se non applicabile.
    val closingDay: Int,
    // Giorno di addebito (es. 15 del mese successivo). 0 se non applicabile.
    val paymentDay: Int,
    val type: CardType,
)
