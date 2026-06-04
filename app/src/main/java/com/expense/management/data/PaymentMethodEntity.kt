package com.expense.management.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expense.management.domain.model.PaymentProvider
import java.util.UUID

@Entity(tableName = "payment_methods")
data class PaymentMethodEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val provider: PaymentProvider,
    val isActive: Boolean = true,
    val issuer: String? = null,
    val currency: String? = null,
)
