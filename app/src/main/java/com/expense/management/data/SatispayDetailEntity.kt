package com.expense.management.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "satispay_details",
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
data class SatispayDetailEntity(
    @PrimaryKey val paymentMethodId: String,
    val weeklyBudget: Double,
    val sddDay: Int = 1,
    val iban: String? = null,
)
