package com.expense.management.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "payment_methods")
data class PaymentMethodEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val provider: String,
    val isActive: Boolean = true,
    val issuer: String? = null,
    val currency: String? = null,
)
