package com.expense.management.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "credit_card_installment_plans",
    foreignKeys = [
        ForeignKey(
            entity = PaymentMethodEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentMethodId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["paymentMethodId"], unique = true),
    ],
)
data class CreditCardInstallmentPlanEntity(
    @PrimaryKey val id: String,
    val paymentMethodId: String,
    val totalAmount: Double,
    val installmentCount: Int,
    val installmentAmount: Double,
    val paidCount: Int = 0,
    val startDate: String,
)
