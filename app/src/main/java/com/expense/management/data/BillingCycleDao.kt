package com.expense.management.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Totali di un ciclo ricavati dalle transazioni collegate: nessun contatore memorizzato. */
data class CycleTotals(
    val purchases: Double,
    val credits: Double,
    val transactionCount: Int,
)

data class CardExposureRow(
    val paymentMethodId: String,
    val exposure: Double,
)

/**
 * Esposizione per carta: spese meno accrediti dei cicli non saldati, meno quanto gia' versato
 * con l'addebito di un ciclo pagato in parte. Solo acquisti con data fino a oggi: le
 * ricorrenze future gia' generate non occupano il plafond.
 */
private const val UNSETTLED_EXPOSURE_BY_CARD =
    "SELECT c.paymentMethodId AS paymentMethodId, " +
        "COALESCE(SUM(CASE WHEN t.type = 'expense' THEN t.amount WHEN t.type = 'income' THEN -t.amount ELSE 0 END), 0) - " +
        "COALESCE((SELECT SUM(s.amount) FROM billing_cycles c2 JOIN transactions s ON s.id = c2.settlementTransactionId " +
        "WHERE c2.paymentMethodId = c.paymentMethodId AND c2.status != 'PAID'), 0) AS exposure " +
        "FROM billing_cycles c " +
        "LEFT JOIN transactions t ON t.billingCycleId = c.id AND t.isCreditCard = 1 AND t.date <= :today " +
        "WHERE c.status != 'PAID' " +
        "GROUP BY c.paymentMethodId"

data class BillingCycleWithTotals(
    @Embedded val cycle: BillingCycleEntity,
    val purchases: Double,
    val credits: Double,
)

@Dao
interface BillingCycleDao {

    // IGNORE e non REPLACE: REPLACE cancellerebbe la riga e scollegherebbe le transazioni
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCycle(cycle: BillingCycleEntity): Long

    @Query("SELECT * FROM billing_cycles WHERE id = :cycleId LIMIT 1")
    suspend fun getCycleById(cycleId: String): BillingCycleEntity?

    /** Cicli aperti la cui data di chiusura e' passata (il giorno di chiusura e' ancora aperto). */
    @Query("SELECT * FROM billing_cycles WHERE status = 'OPEN' AND closingDate < :today")
    suspend fun getCyclesToClose(today: String): List<BillingCycleEntity>

    @Query("SELECT * FROM billing_cycles WHERE status = 'CLOSED' AND dueDate <= :today ORDER BY dueDate")
    suspend fun getCyclesToSettle(today: String): List<BillingCycleEntity>

    @Query("UPDATE billing_cycles SET status = :status WHERE id = :cycleId")
    suspend fun updateStatus(cycleId: String, status: String)

    @Query("UPDATE billing_cycles SET status = 'PAID', settlementTransactionId = :settlementTransactionId WHERE id = :cycleId")
    suspend fun markSettled(cycleId: String, settlementTransactionId: String?)

    @Query(
        "SELECT " +
            "COALESCE(SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END), 0) AS purchases, " +
            "COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) AS credits, " +
            "COUNT(*) AS transactionCount " +
            "FROM transactions WHERE billingCycleId = :cycleId AND isCreditCard = 1",
    )
    suspend fun getCycleTotals(cycleId: String): CycleTotals

    /** Cicli non ancora saldati della carta (OPEN, CLOSED, PARTIALLY_PAID), dal piu' vecchio. */
    @Query(
        "SELECT c.*, " +
            "COALESCE(SUM(CASE WHEN t.type = 'expense' THEN t.amount ELSE 0 END), 0) AS purchases, " +
            "COALESCE(SUM(CASE WHEN t.type = 'income' THEN t.amount ELSE 0 END), 0) AS credits " +
            "FROM billing_cycles c " +
            "LEFT JOIN transactions t ON t.billingCycleId = c.id AND t.isCreditCard = 1 " +
            "WHERE c.paymentMethodId = :paymentMethodId AND c.status != 'PAID' " +
            "GROUP BY c.id ORDER BY c.cycleMonth",
    )
    suspend fun getUnsettledCyclesWithTotals(paymentMethodId: String): List<BillingCycleWithTotals>

    @Query(UNSETTLED_EXPOSURE_BY_CARD)
    fun getUnsettledExposureByCardFlow(today: String): Flow<List<CardExposureRow>>

    @Query(UNSETTLED_EXPOSURE_BY_CARD)
    suspend fun getUnsettledExposureByCard(today: String): List<CardExposureRow>
}
