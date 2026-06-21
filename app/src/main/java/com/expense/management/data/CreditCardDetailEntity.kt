package com.expense.management.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "credit_card_details",
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
data class CreditCardDetailEntity(
    @PrimaryKey val paymentMethodId: String,
    val cardType: String,
    val limit: Double,
    val closingDay: Int = 0,
    val paymentDay: Int = 0,
    val interestRate: Double? = null,
    val issuer: String? = null,
    val linkedPaymentMethodId: String? = null,
)
