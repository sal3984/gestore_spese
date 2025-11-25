package com.alessandrogregorio.gestorespese.data

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val description: String,
    val amount: Double,
    val categoryId: String,
    val type: String, // "expense" o "income"
    val isCreditCard: Boolean,
    val effectiveDate: String // Data reale di addebito
)


