package com.expense.management.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Totali di un ciclo ricavati dalle transazioni collegate: nessun contatore memorizzato. */
data class CycleTotals(
    val purchases: Double,
    val credits: Double,
    val transactionCount: Int,
)

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

    /**
     * Esposizione della carta: spese meno accrediti dei cicli non saldati, meno quanto gia'
     * versato con l'addebito di saldo di un ciclo pagato in parte.
     */
    @Query(
        "SELECT " +
            "COALESCE((SELECT SUM(CASE WHEN t.type = 'expense' THEN t.amount ELSE -t.amount END) " +
            "FROM transactions t JOIN billing_cycles c ON t.billingCycleId = c.id " +
            "WHERE c.paymentMethodId = :paymentMethodId AND c.status != 'PAID' AND t.isCreditCard = 1), 0) - " +
            "COALESCE((SELECT SUM(s.amount) FROM billing_cycles c JOIN transactions s ON s.id = c.settlementTransactionId " +
            "WHERE c.paymentMethodId = :paymentMethodId AND c.status != 'PAID'), 0)",
    )
    suspend fun getUnsettledExposure(paymentMethodId: String): Double
}
