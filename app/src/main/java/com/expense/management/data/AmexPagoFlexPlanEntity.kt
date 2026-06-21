package com.expense.management.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "amex_pagoflex_plans",
    foreignKeys = [
        ForeignKey(
            entity = AmexStatementEntity::class,
            parentColumns = ["id"],
            childColumns = ["statementId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["statementId"]),
        Index(value = ["transactionId"], unique = true),
    ],
)
data class AmexPagoFlexPlanEntity(
    @PrimaryKey val id: String,
    val statementId: String,
    val transactionId: String,
    val totalAmount: Double,
    val installmentCount: Int,
    val installmentAmount: Double,
    val paidCount: Int = 0,
    val startDate: String,
    val planType: String = "FIXED_DURATION",
    val initialInstallmentAmount: Double? = null,
)
