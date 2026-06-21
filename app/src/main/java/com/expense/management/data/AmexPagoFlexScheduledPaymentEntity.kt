package com.expense.management.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "amex_pagoflex_scheduled_payments",
    foreignKeys = [
        ForeignKey(
            entity = AmexPagoFlexPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["planId"]),
        Index(value = ["dueDate"]),
        Index(value = ["status"]),
    ],
)
data class AmexPagoFlexScheduledPaymentEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val sequenceNumber: Int,
    val dueDate: String,
    val amount: Double,
    val status: String,
    val expenseTransactionId: String? = null,
)
