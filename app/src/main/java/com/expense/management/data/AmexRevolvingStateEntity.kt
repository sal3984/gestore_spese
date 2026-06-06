package com.expense.management.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "amex_revolving_balances",
    foreignKeys = [
        ForeignKey(
            entity = AmexStatementEntity::class,
            parentColumns = ["id"],
            childColumns = ["statementId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["statementId"], unique = true),
    ],
)
data class AmexRevolvingStateEntity(
    @PrimaryKey val id: String,
    val statementId: String,
    val carriedForwardDebt: Double,
    val interestCharged: Double = 0.0,
    val interestRate: Double = 0.0,
    val userPaymentChoice: String,
    val paymentAmount: Double = 0.0,
)
