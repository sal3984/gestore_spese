package com.expense.management.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val label: String,
    val icon: String,
    // "income" o "expense"
    val type: String,
    // Per distinguere quelle default da quelle utente
    val isCustom: Boolean = false,
)
