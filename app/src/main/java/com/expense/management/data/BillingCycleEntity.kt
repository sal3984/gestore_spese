package com.expense.management.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expense.management.domain.model.CyclePaymentMode
import com.expense.management.domain.model.CycleStatus
import com.expense.management.domain.usecase.CalculateBillingCycleUseCase
import java.time.LocalDate

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

        /** sourceRef dell'unico addebito sul conto che salda il ciclo. */
        fun settlementRef(cycleId: String): String = "$SETTLEMENT_PREFIX$cycleId$SETTLEMENT_SUFFIX"

        private const val SETTLEMENT_PREFIX = "cycle:"
        private const val SETTLEMENT_SUFFIX = ":settlement"

        internal fun isSettlementRef(sourceRef: String?): Boolean =
            sourceRef != null && sourceRef.startsWith(SETTLEMENT_PREFIX) && sourceRef.endsWith(SETTLEMENT_SUFFIX)

        /**
         * Nuovo ciclo a saldo per un acquisto, con lo stato dato da oggi: addebito passato ->
         * PAID (fino alla Fase 2 ogni acquisto esce dal conto alla sua effectiveDate),
         * chiusura passata -> CLOSED, altrimenti OPEN. Usato dalla migration 23->24 e al salvataggio.
         */
        fun forPurchase(paymentMethodId: String, dates: CalculateBillingCycleUseCase.CycleDates, today: LocalDate): BillingCycleEntity {
            val status = when {
                !today.isBefore(dates.dueDate) -> CycleStatus.PAID
                today.isAfter(dates.closingDate) -> CycleStatus.CLOSED
                else -> CycleStatus.OPEN
            }
            return BillingCycleEntity(
                id = idFor(paymentMethodId, dates.cycleMonth.toString()),
                paymentMethodId = paymentMethodId,
                cycleMonth = dates.cycleMonth.toString(),
                closingDate = dates.closingDate.toString(),
                dueDate = dates.dueDate.toString(),
                status = status.name,
                paymentMode = CyclePaymentMode.SALDO.name,
            )
        }
    }
}

/**
 * Addebito sul conto che salda un ciclo: e' un trasferimento, non una spesa. Le spese della
 * carta sono gia' contate una per una alla loro effectiveDate, quindi spese e report lo escludono.
 */
val TransactionEntity.isCycleSettlement: Boolean
    get() = BillingCycleEntity.isSettlementRef(sourceRef)
