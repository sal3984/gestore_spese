package com.expense.management.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "debit_card_details",
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
data class DebitCardDetailEntity(
    @PrimaryKey val paymentMethodId: String,
    val issuer: String? = null,
    val cardNumber: String? = null,
    val notes: String? = null,
)
