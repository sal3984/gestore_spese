package com.alessandrogregorio.gestorespese.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val label: String,
    val icon: String,
    val type: String, // "expense" o "income"
    val isCustom: Boolean = false // Per distinguere quelle default da quelle utente
)
