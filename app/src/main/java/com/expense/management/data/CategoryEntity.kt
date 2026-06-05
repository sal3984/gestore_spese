package com.expense.management.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["type"])],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val label: String,
    val icon: String,
    // "expense" o "income"
    val type: TransactionType,
    // Per distinguere quelle default da quelle utente
    val isCustom: Boolean = false,
    // URI locale per immagini personalizzate
    val imageUri: String? = null,
)
