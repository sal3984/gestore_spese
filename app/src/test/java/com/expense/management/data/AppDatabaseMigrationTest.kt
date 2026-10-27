package com.expense.management.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Test delle migration Room su JVM (Robolectric), con fixture sintetiche.
 * Ogni test parte dallo schema esportato in app/schemas/ e verifica sia i dati
 * sia lo schema risultante.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migration17To18_keepsPaidInstallmentsAndDatesFromPlanStart() {
        helper.createDatabase(TEST_DB, 17).use { db ->
            db.insertPaymentMethod("card1", "CREDIT_CARD_INSTALLMENT")
            db.insertInstallmentPlanV17("plan1", "card1", total = 600.0, count = 6, amount = 100.0, paid = 2, startDate = "2025-11-15")
        }

        helper.runMigrationsAndValidate(TEST_DB, 18, true, AppDatabase.MIGRATION_17_18).use { db ->
            val rows = db.scheduledRows("installment_scheduled_payments", "plan1")

            assertEquals(6, rows.size)
            assertEquals(
                listOf("2025-11-15", "2025-12-15", "2026-01-15", "2026-02-15", "2026-03-15", "2026-04-15"),
                rows.map { it.dueDate },
            )
            assertEquals(listOf("PAID", "PAID", "PENDING", "PENDING", "PENDING", "PENDING"), rows.map { it.status })
            assertEquals(2, db.intValue("SELECT paidCount FROM credit_card_installment_plans WHERE id = 'plan1'"))
        }
    }

    @Test
    fun migration17To18_putsRoundingOnLastInstallmentAndClampsDayTo28() {
        helper.createDatabase(TEST_DB, 17).use { db ->
            db.insertPaymentMethod("card1", "CREDIT_CARD_INSTALLMENT")
            db.insertInstallmentPlanV17("plan1", "card1", total = 100.0, count = 3, amount = 33.33, paid = 0, startDate = "2025-01-31")
        }

        helper.runMigrationsAndValidate(TEST_DB, 18, true, AppDatabase.MIGRATION_17_18).use { db ->
            val rows = db.scheduledRows("installment_scheduled_payments", "plan1")

            assertEquals(listOf("2025-01-28", "2025-02-28", "2025-03-28"), rows.map { it.dueDate })
            assertEquals(33.33, rows[0].amount, 0.001)
            assertEquals(33.33, rows[1].amount, 0.001)
            assertEquals(33.34, rows[2].amount, 0.001)
        }
    }

    @Test
    fun migration19To20_numbersAmexInstallmentsFromOne() {
        helper.createDatabase(TEST_DB, 19).use { db ->
            db.insertPaymentMethod("amex1", "CREDIT_CARD_AMEX")
            db.execSQL(
                "INSERT INTO amex_statements (id, paymentMethodId, statementMonth, totalExpenses, totalPagoflex, " +
                    "revolvingBalance, paymentMode, paymentAmount, closingDate, paymentDueDate, isClosed) " +
                    "VALUES ('stmt1', 'amex1', '2025-02', 300.0, 300.0, 0.0, 'SALDO', 300.0, '2025-02-28', '2025-03-10', 0)",
            )
            db.execSQL(
                "INSERT INTO amex_pagoflex_plans (id, statementId, transactionId, totalAmount, installmentCount, " +
                    "installmentAmount, paidCount, startDate) " +
                    "VALUES ('plan1', 'stmt1', 'tx1', 300.0, 3, 100.0, 1, '2025-03-10')",
            )
        }

        helper.runMigrationsAndValidate(TEST_DB, 20, true, AppDatabase.MIGRATION_19_20).use { db ->
            val rows = db.scheduledRows("amex_pagoflex_scheduled_payments", "plan1")

            assertEquals(listOf(1, 2, 3), rows.map { it.sequenceNumber })
            assertEquals(listOf("2025-03-10", "2025-04-10", "2025-05-10"), rows.map { it.dueDate })
            assertEquals(listOf("PAID", "PENDING", "PENDING"), rows.map { it.status })
        }
    }

    @Test
    fun migration22To23_renumbersZeroBasedAmexInstallmentsAndTheirTransactions() {
        helper.createDatabase(TEST_DB, 22).use { db ->
            db.insertAmexPlanV22("plan1", installmentCount = 3)
            db.insertTransactionV22("tx0", "Rata Amex 0/3", installmentNumber = 0)
            db.insertTransactionV22("tx1", "Rata di aprile", installmentNumber = 1)
            db.insertAmexScheduledPayment("p0", "plan1", 0, "2025-03-10", "PAID", "tx0")
            db.insertAmexScheduledPayment("p1", "plan1", 1, "2025-04-10", "PAID", "tx1")
            db.insertAmexScheduledPayment("p2", "plan1", 2, "2025-05-10", "PENDING", null)
        }

        helper.runMigrationsAndValidate(TEST_DB, 23, true, AppDatabase.MIGRATION_22_23).use { db ->
            assertEquals(listOf(1, 2, 3), db.scheduledRows("amex_pagoflex_scheduled_payments", "plan1").map { it.sequenceNumber })
            assertEquals("Rata Amex 1/3" to 1, db.transactionLabel("tx0"))
            // Descrizione modificata a mano: si corregge solo il numero di rata
            assertEquals("Rata di aprile" to 2, db.transactionLabel("tx1"))
        }
    }

    @Test
    fun migration22To23_renumbersByDueDateAfterARecalculationAndIsIdempotent() {
        helper.createDatabase(TEST_DB, 22).use { db ->
            db.insertAmexPlanV22("plan1", installmentCount = 3)
            // Il ricalcolo ha gia' rinumerato le pendenti a partire da paidCount + 1
            db.insertAmexScheduledPayment("p0", "plan1", 0, "2025-03-10", "PAID", null)
            db.insertAmexScheduledPayment("p1", "plan1", 2, "2025-04-10", "PENDING", null)
            db.insertAmexScheduledPayment("p2", "plan1", 3, "2025-05-10", "PENDING", null)
        }

        helper.runMigrationsAndValidate(TEST_DB, 23, true, AppDatabase.MIGRATION_22_23).use { db ->
            assertEquals(listOf(1, 2, 3), db.scheduledRows("amex_pagoflex_scheduled_payments", "plan1").map { it.sequenceNumber })

            AppDatabase.MIGRATION_22_23.migrate(db)

            assertEquals(listOf(1, 2, 3), db.scheduledRows("amex_pagoflex_scheduled_payments", "plan1").map { it.sequenceNumber })
        }
    }

    @Test
    fun migration22To23_leavesHealthyPlansAndUnlinkedTransactionsUntouched() {
        helper.createDatabase(TEST_DB, 22).use { db ->
            db.insertAmexPlanV22("plan1", installmentCount = 2)
            db.insertTransactionV22("tx1", "Rata Amex 1/2", installmentNumber = 1)
            db.insertTransactionV22("txManual", "Rata Amex 0/3", installmentNumber = 0)
            db.insertAmexScheduledPayment("p1", "plan1", 1, "2025-03-10", "PAID", "tx1")
            db.insertAmexScheduledPayment("p2", "plan1", 2, "2025-04-10", "PENDING", null)
        }

        helper.runMigrationsAndValidate(TEST_DB, 23, true, AppDatabase.MIGRATION_22_23).use { db ->
            assertEquals(listOf(1, 2), db.scheduledRows("amex_pagoflex_scheduled_payments", "plan1").map { it.sequenceNumber })
            assertEquals("Rata Amex 1/2" to 1, db.transactionLabel("tx1"))
            assertEquals("Rata Amex 0/3" to 0, db.transactionLabel("txManual"))
        }
    }

    @Test
    fun migration23To24_assignsSaldoPurchasesToCyclesRecomputedFromThePurchaseDate() {
        helper.createDatabase(TEST_DB, 23).use { db ->
            db.insertCard("saldo1", "SALDO", closingDay = 20, paymentDay = 15)
            db.insertTransactionV23("before", "2025-05-10", effectiveDate = "2025-06-15", paymentMethodId = "saldo1")
            // Salvata con la regola vecchia: il giorno di chiusura andava al ciclo successivo
            db.insertTransactionV23("onClosing", "2025-05-20", effectiveDate = "2025-07-15", paymentMethodId = "saldo1")
            db.insertTransactionV23("after", "2025-05-21", effectiveDate = "2025-07-15", paymentMethodId = "saldo1")
            db.insertTransactionV23("open", "2025-06-24", effectiveDate = "2025-08-15", paymentMethodId = "saldo1")
            db.insertTransactionV23("legacy", "2025-05-12", effectiveDate = "2025-06-15", creditCardId = "saldo1")
        }

        helper.runMigrationsAndValidate(TEST_DB, 24, true, AppDatabase.migration23To24(today = LocalDate.of(2025, 6, 25))).use { db ->
            assertEquals(
                listOf(
                    CycleRow("saldo1:2025-05", "saldo1", "2025-05", "2025-05-20", "2025-06-15", "PAID", "SALDO"),
                    CycleRow("saldo1:2025-06", "saldo1", "2025-06", "2025-06-20", "2025-07-15", "CLOSED", "SALDO"),
                    CycleRow("saldo1:2025-07", "saldo1", "2025-07", "2025-07-20", "2025-08-15", "OPEN", "SALDO"),
                ),
                db.cycleRows(),
            )
            assertEquals(
                mapOf(
                    "before" to "saldo1:2025-05",
                    "onClosing" to "saldo1:2025-05",
                    "after" to "saldo1:2025-06",
                    "open" to "saldo1:2025-07",
                    "legacy" to "saldo1:2025-05",
                ),
                db.cycleAssignments(),
            )
            // Salvata con la regola vecchia al 15/07: ora cade con l'addebito del suo ciclo
            assertEquals("2025-06-15", db.stringValue("SELECT effectiveDate FROM transactions WHERE id = 'onClosing'"))
            assertEquals(
                0,
                db.intValue(
                    "SELECT COUNT(*) FROM transactions t JOIN billing_cycles c ON t.billingCycleId = c.id " +
                        "WHERE t.effectiveDate != c.dueDate",
                ),
            )
            assertEquals(0, db.intValue("SELECT COUNT(*) FROM pragma_foreign_key_check"))
        }
    }

    @Test
    fun migration23To24_leavesNonSaldoAndNonPurchaseTransactionsWithoutCycle() {
        helper.createDatabase(TEST_DB, 23).use { db ->
            db.insertCard("saldo1", "SALDO", closingDay = 20, paymentDay = 15)
            db.insertCard("noDays", "SALDO", closingDay = 0, paymentDay = 0)
            db.insertCard("revolving1", "REVOLVING", closingDay = 20, paymentDay = 15)
            db.insertCard("amex1", "AMEX_HYBRID", closingDay = 20, paymentDay = 15)
            db.insertPaymentMethod("bank1", "CASH")
            db.insertTransactionV23("refund", "2025-05-10", type = "income", paymentMethodId = "saldo1")
            db.insertTransactionV23("cash", "2025-05-10", isCreditCard = false, paymentMethodId = "bank1")
            db.insertTransactionV23("noDaysCard", "2025-05-10", paymentMethodId = "noDays")
            db.insertTransactionV23("revolving", "2025-05-10", paymentMethodId = "revolving1")
            db.insertTransactionV23("amex", "2025-05-10", paymentMethodId = "amex1")
            db.insertTransactionV23("badDate", "10/05/2025", paymentMethodId = "saldo1")
            // paymentMethodId che non e' una carta: la carta si trova in creditCardId
            db.insertTransactionV23("viaLegacyColumn", "2025-05-10", paymentMethodId = "bank1", creditCardId = "saldo1")
        }

        helper.runMigrationsAndValidate(TEST_DB, 24, true, AppDatabase.migration23To24(today = LocalDate.of(2025, 6, 25))).use { db ->
            assertEquals(mapOf("viaLegacyColumn" to "saldo1:2025-05"), db.cycleAssignments())
            assertEquals(listOf("saldo1:2025-05"), db.cycleRows().map { it.id })
        }
    }

    @Test
    fun migration23To24_sourceRefIsUniqueAndDeletingTheCardUnlinksItsTransactions() {
        helper.createDatabase(TEST_DB, 23).use { db ->
            db.insertCard("saldo1", "SALDO", closingDay = 20, paymentDay = 15)
            db.insertTransactionV23("purchase", "2025-05-10", paymentMethodId = "saldo1")
        }

        helper.runMigrationsAndValidate(TEST_DB, 24, true, AppDatabase.migration23To24(today = LocalDate.of(2025, 6, 25))).use { db ->
            db.execSQL("UPDATE transactions SET sourceRef = 'cycle:x:settlement' WHERE id = 'purchase'")
            db.insertTransactionV23("other", "2025-05-11", isCreditCard = false)
            assertThrows(SQLiteConstraintException::class.java) {
                db.execSQL("UPDATE transactions SET sourceRef = 'cycle:x:settlement' WHERE id = 'other'")
            }

            db.execSQL("PRAGMA foreign_keys = ON")
            db.execSQL("DELETE FROM payment_methods WHERE id = 'saldo1'")

            assertEquals(0, db.intValue("SELECT COUNT(*) FROM billing_cycles"))
            assertEquals(emptyMap<String, String>(), db.cycleAssignments())
            assertEquals(2, db.intValue("SELECT COUNT(*) FROM transactions"))
        }
    }

    @Test
    fun allMigrationsFromOldestExportedSchema_matchTheCurrentRoomSchema() {
        helper.createDatabase(TEST_DB, OLDEST_EXPORTED_VERSION).close()

        helper.runMigrationsAndValidate(TEST_DB, LATEST_VERSION, true, *AppDatabase.ALL_MIGRATIONS).close()

        // Apertura con le entity reali: Room fallisce se lo schema migrato non coincide
        val context = ApplicationProvider.getApplicationContext<Context>()
        Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()
            .apply {
                openHelper.writableDatabase
                close()
            }
    }

    private data class ScheduledRow(
        val sequenceNumber: Int?,
        val dueDate: String,
        val amount: Double,
        val status: String,
    )

    private fun SupportSQLiteDatabase.insertPaymentMethod(id: String, provider: String) {
        execSQL(
            "INSERT INTO payment_methods (id, name, provider, isActive) VALUES (?, ?, ?, 1)",
            arrayOf(id, id, provider),
        )
    }

    private fun SupportSQLiteDatabase.insertInstallmentPlanV17(
        id: String,
        paymentMethodId: String,
        total: Double,
        count: Int,
        amount: Double,
        paid: Int,
        startDate: String,
    ) {
        execSQL(
            "INSERT INTO credit_card_installment_plans (id, transactionId, paymentMethodId, totalAmount, " +
                "installmentCount, installmentAmount, paidCount, startDate) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any>(id, "tx_$id", paymentMethodId, total, count, amount, paid, startDate),
        )
    }

    private fun SupportSQLiteDatabase.insertAmexPlanV22(id: String, installmentCount: Int) {
        insertPaymentMethod("amex_$id", "CREDIT_CARD_AMEX")
        execSQL(
            "INSERT INTO amex_statements (id, paymentMethodId, statementMonth, totalExpenses, totalPagoflex, " +
                "revolvingBalance, paymentMode, paymentAmount, closingDate, paymentDueDate, isClosed) " +
                "VALUES (?, ?, '2025-02', 300.0, 300.0, 0.0, 'SALDO', 300.0, '2025-02-28', '2025-03-10', 1)",
            arrayOf("stmt_$id", "amex_$id"),
        )
        execSQL(
            "INSERT INTO amex_pagoflex_plans (id, statementId, transactionId, totalAmount, installmentCount, " +
                "installmentAmount, paidCount, startDate, planType) VALUES (?, ?, ?, 300.0, ?, 100.0, 0, '2025-03-10', 'FIXED_DURATION')",
            arrayOf<Any>(id, "stmt_$id", "purchase_$id", installmentCount),
        )
    }

    private fun SupportSQLiteDatabase.insertAmexScheduledPayment(
        id: String,
        planId: String,
        sequenceNumber: Int,
        dueDate: String,
        status: String,
        expenseTransactionId: String?,
    ) {
        execSQL(
            "INSERT INTO amex_pagoflex_scheduled_payments (id, planId, sequenceNumber, dueDate, amount, status, " +
                "expenseTransactionId) VALUES (?, ?, ?, ?, 100.0, ?, ?)",
            arrayOf<Any?>(id, planId, sequenceNumber, dueDate, status, expenseTransactionId),
        )
    }

    private fun SupportSQLiteDatabase.insertTransactionV22(id: String, description: String, installmentNumber: Int) {
        execSQL(
            "INSERT INTO transactions (id, date, description, amount, categoryId, type, isCreditCard, effectiveDate, " +
                "originalAmount, originalCurrency, installmentNumber, recurrenceType) " +
                "VALUES (?, '2025-03-10', ?, 100.0, 'credit_card_payment', 'EXPENSE', 0, '2025-03-10', 100.0, 'EUR', ?, 'NONE')",
            arrayOf<Any>(id, description, installmentNumber),
        )
    }

    private fun SupportSQLiteDatabase.transactionLabel(id: String): Pair<String, Int> =
        query("SELECT description, installmentNumber FROM transactions WHERE id = ?", arrayOf(id)).use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0) to cursor.getInt(1)
        }

    private data class CycleRow(
        val id: String,
        val paymentMethodId: String,
        val cycleMonth: String,
        val closingDate: String,
        val dueDate: String,
        val status: String,
        val paymentMode: String,
    )

    private fun SupportSQLiteDatabase.insertCard(id: String, cardType: String, closingDay: Int, paymentDay: Int) {
        insertPaymentMethod(id, "CREDIT_CARD_$cardType")
        execSQL(
            "INSERT INTO credit_card_details (paymentMethodId, cardType, `limit`, closingDay, paymentDay) VALUES (?, ?, 3000.0, ?, ?)",
            arrayOf<Any>(id, cardType, closingDay, paymentDay),
        )
    }

    private fun SupportSQLiteDatabase.insertTransactionV23(
        id: String,
        date: String,
        effectiveDate: String = date,
        type: String = "expense",
        isCreditCard: Boolean = true,
        paymentMethodId: String? = null,
        creditCardId: String? = null,
    ) {
        execSQL(
            "INSERT INTO transactions (id, date, description, amount, categoryId, type, isCreditCard, effectiveDate, " +
                "originalAmount, originalCurrency, creditCardId, paymentMethodId, recurrenceType) " +
                "VALUES (?, ?, ?, 50.0, 'shopping', ?, ?, ?, 50.0, 'EUR', ?, ?, 'NONE')",
            arrayOf<Any?>(id, date, id, type, if (isCreditCard) 1 else 0, effectiveDate, creditCardId, paymentMethodId),
        )
    }

    private fun SupportSQLiteDatabase.cycleRows(): List<CycleRow> =
        query("SELECT id, paymentMethodId, cycleMonth, closingDate, dueDate, status, paymentMode FROM billing_cycles ORDER BY id").use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(CycleRow(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6)))
                }
            }
        }

    private fun SupportSQLiteDatabase.cycleAssignments(): Map<String, String> =
        query("SELECT id, billingCycleId FROM transactions WHERE billingCycleId IS NOT NULL").use { cursor ->
            buildMap {
                while (cursor.moveToNext()) put(cursor.getString(0), cursor.getString(1))
            }
        }

    private fun SupportSQLiteDatabase.stringValue(sql: String): String = query(sql).use { cursor ->
        cursor.moveToFirst()
        cursor.getString(0)
    }

    private fun SupportSQLiteDatabase.scheduledRows(table: String, planId: String): List<ScheduledRow> {
        val hasSequence = table == "amex_pagoflex_scheduled_payments"
        val sequenceColumn = if (hasSequence) "sequenceNumber" else "NULL"
        return query(
            "SELECT $sequenceColumn, dueDate, amount, status FROM $table WHERE planId = ? ORDER BY dueDate",
            arrayOf(planId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        ScheduledRow(
                            sequenceNumber = if (cursor.isNull(0)) null else cursor.getInt(0),
                            dueDate = cursor.getString(1),
                            amount = cursor.getDouble(2),
                            status = cursor.getString(3),
                        ),
                    )
                }
            }
        }
    }

    private fun SupportSQLiteDatabase.intValue(sql: String): Int = query(sql).use { cursor ->
        cursor.moveToFirst()
        cursor.getInt(0)
    }

    private companion object {
        const val TEST_DB = "migration-test"
        const val OLDEST_EXPORTED_VERSION = 16
        const val LATEST_VERSION = DATABASE_VERSION
    }
}
