package com.expense.management.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "amex_statements",
    foreignKeys = [
        ForeignKey(
            entity = PaymentMethodEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentMethodId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["paymentMethodId", "statementMonth"], unique = true),
    ],
)
data class AmexStatementEntity(
    @PrimaryKey val id: String,
    val paymentMethodId: String,
    val statementMonth: String,
    val totalExpenses: Double = 0.0,
    val totalPagoflex: Double = 0.0,
    val revolvingBalance: Double = 0.0,
    val paymentMode: String,
    val paymentAmount: Double = 0.0,
    val closingDate: String,
    val paymentDueDate: String,
    val isClosed: Boolean = false,
)
