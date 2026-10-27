package com.expense.management.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
        const val LATEST_VERSION = 22
    }
}
