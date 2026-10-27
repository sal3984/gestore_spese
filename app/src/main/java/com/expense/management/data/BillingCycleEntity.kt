package com.expense.management.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Ciclo di fatturazione di una carta. Nessun totale: il dovuto si ricava dalle transazioni
 * con lo stesso billingCycleId. Id deterministico "<paymentMethodId>:<yyyy-MM>".
 * Non usare INSERT OR REPLACE: cancellerebbe la riga e azzererebbe il billingCycleId delle
 * transazioni collegate (ON DELETE SET NULL).
 */
@Entity(
    tableName = "billing_cycles",
    foreignKeys = [
        ForeignKey(
            entity = PaymentMethodEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentMethodId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["paymentMethodId", "cycleMonth"], unique = true),
    ],
)
data class BillingCycleEntity(
    @PrimaryKey val id: String,
    val paymentMethodId: String,
    // yyyy-MM
    val cycleMonth: String,
    val closingDate: String,
    val dueDate: String,
    val status: String,
    val paymentMode: String,
    val chosenAmount: Double? = null,
    val settlementTransactionId: String? = null,
    val carriedForward: Double? = null,
) {
    companion object {
        fun idFor(paymentMethodId: String, cycleMonth: String): String = "$paymentMethodId:$cycleMonth"
    }
}
