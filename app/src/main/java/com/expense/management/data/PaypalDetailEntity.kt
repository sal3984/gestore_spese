package com.expense.management.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "paypal_details",
    foreignKeys = [
        ForeignKey(
            entity = PaymentMethodEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentMethodId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["paymentMethodId"], unique = true)],
)
data class PaypalDetailEntity(
    @PrimaryKey val paymentMethodId: String,
    val email: String,
    val bnplInstallmentCount: Int = 3,
    val bnplCycleDays: Int = 14,
)
