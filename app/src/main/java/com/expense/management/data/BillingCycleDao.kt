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
 * Esposizione per carta: per ogni ciclo, spese meno accrediti con data fino a oggi (le
 * ricorrenze future gia' generate non occupano il plafond), meno quanto gia' addebitato.
 * Cicli non saldati: contano per intero. Cicli PAID con addebito: conta solo la parte di spese
 * non ancora addebitata (spesa aggiunta dopo l'addebito), mai un addebito in eccesso.
 * Cicli PAID senza addebito (saldati prima dell'aggiornamento o a zero): non contano.
 * Compaiono solo le carte con almeno un ciclo che conta.
 */
private const val UNSETTLED_EXPOSURE_BY_CARD =
    "SELECT paymentMethodId, " +
        "SUM(CASE WHEN status != 'PAID' THEN owed WHEN settlementId IS NOT NULL AND owed > 0.005 THEN owed ELSE 0 END) AS exposure " +
        "FROM (" +
        "SELECT c.paymentMethodId AS paymentMethodId, c.status AS status, c.settlementTransactionId AS settlementId, " +
        "COALESCE(SUM(CASE WHEN t.type = 'expense' THEN t.amount WHEN t.type = 'income' THEN -t.amount ELSE 0 END), 0) - " +
        "COALESCE((SELECT s.amount FROM transactions s WHERE s.id = c.settlementTransactionId), 0) AS owed " +
        "FROM billing_cycles c " +
        "LEFT JOIN transactions t ON t.billingCycleId = c.id AND t.isCreditCard = 1 AND t.date <= :today " +
        "GROUP BY c.id" +
        ") " +
        "GROUP BY paymentMethodId " +
        "HAVING SUM(CASE WHEN status != 'PAID' OR (settlementId IS NOT NULL AND owed > 0.005) THEN 1 ELSE 0 END) > 0"

/** Totale di un ciclo non saldato: spese meno accrediti con data fino a oggi. */
data class UnsettledCycleTotalRow(
    val paymentMethodId: String,
    val dueDate: String,
    val amount: Double,
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

    @Query("SELECT * FROM billing_cycles")
    suspend fun getAllCycles(): List<BillingCycleEntity>

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

    @Query("SELECT * FROM billing_cycles WHERE paymentMethodId = :paymentMethodId ORDER BY cycleMonth DESC")
    fun getCyclesForCardFlow(paymentMethodId: String): Flow<List<BillingCycleEntity>>

    /** Acquisti e accrediti collegati ai cicli della carta. */
    @Query(
        "SELECT t.* FROM transactions t JOIN billing_cycles c ON t.billingCycleId = c.id " +
            "WHERE c.paymentMethodId = :paymentMethodId AND t.isCreditCard = 1",
    )
    fun getCycleTransactionsForCardFlow(paymentMethodId: String): Flow<List<TransactionEntity>>

    /** Addebiti sul conto che hanno saldato i cicli della carta. */
    @Query(
        "SELECT s.* FROM transactions s JOIN billing_cycles c ON s.id = c.settlementTransactionId " +
            "WHERE c.paymentMethodId = :paymentMethodId",
    )
    fun getSettlementsForCardFlow(paymentMethodId: String): Flow<List<TransactionEntity>>

    @Query(
        "SELECT c.paymentMethodId AS paymentMethodId, c.dueDate AS dueDate, " +
            "COALESCE(SUM(CASE WHEN t.type = 'expense' THEN t.amount WHEN t.type = 'income' THEN -t.amount ELSE 0 END), 0) AS amount " +
            "FROM billing_cycles c " +
            "LEFT JOIN transactions t ON t.billingCycleId = c.id AND t.isCreditCard = 1 AND t.date <= :today " +
            "WHERE c.status != 'PAID' " +
            "GROUP BY c.id",
    )
    fun getUnsettledCycleTotalsFlow(today: String): Flow<List<UnsettledCycleTotalRow>>

    @Query(UNSETTLED_EXPOSURE_BY_CARD)
    fun getUnsettledExposureByCardFlow(today: String): Flow<List<CardExposureRow>>

    @Query(UNSETTLED_EXPOSURE_BY_CARD)
    suspend fun getUnsettledExposureByCard(today: String): List<CardExposureRow>
}
