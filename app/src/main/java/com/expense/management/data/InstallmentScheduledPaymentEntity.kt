package com.expense.management.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "installment_scheduled_payments",
    foreignKeys = [
        ForeignKey(
            entity = CreditCardInstallmentPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["planId"]),
    ],
)
data class InstallmentScheduledPaymentEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val dueDate: String,
    val amount: Double,
    val status: String,
    val expenseTransactionId: String? = null,
)
