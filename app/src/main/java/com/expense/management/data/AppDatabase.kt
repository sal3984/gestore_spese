package com.expense.management.data

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        CurrencyRate::class,
        PaymentMethodEntity::class,
        CreditCardDetailEntity::class,
        RevolutDetailEntity::class,
        SatispayDetailEntity::class,
        PaypalDetailEntity::class,
        KlarnaDetailEntity::class,
        DebitCardDetailEntity::class,
        CreditCardInstallmentPlanEntity::class,
        InstallmentScheduledPaymentEntity::class,
        AmexStatementEntity::class,
        AmexPagoFlexPlanEntity::class,
        AmexPagoFlexScheduledPaymentEntity::class,
        AmexPagoFlexPlanChangeEntity::class,
        AmexRevolvingStateEntity::class,
    ],
    version = 22,
    exportSchema = true,
)
@TypeConverters(TransactionTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    abstract fun categoryDao(): CategoryDao

    abstract fun currencyDao(): CurrencyDao

    abstract fun paymentMethodDao(): PaymentMethodDao

    abstract fun amexDao(): AmexDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `payment_methods` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `provider` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `issuer` TEXT DEFAULT NULL,
                        `currency` TEXT DEFAULT NULL,
                        PRIMARY KEY(`id`)
                    )
                """,
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `credit_card_details` (
                        `paymentMethodId` TEXT NOT NULL,
                        `cardType` TEXT NOT NULL,
                        `limit` REAL NOT NULL,
                        `closingDay` INTEGER NOT NULL DEFAULT 0,
                        `paymentDay` INTEGER NOT NULL DEFAULT 0,
                        `interestRate` REAL DEFAULT NULL,
                        `issuer` TEXT DEFAULT NULL,
                        PRIMARY KEY(`paymentMethodId`),
                        FOREIGN KEY(`paymentMethodId`) REFERENCES `payment_methods`(`id`) ON DELETE CASCADE
                    )
                """,
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `revolut_details` (
                        `paymentMethodId` TEXT NOT NULL,
                        `currency` TEXT NOT NULL DEFAULT 'EUR',
                        `iban` TEXT DEFAULT NULL,
                        `accountNumber` TEXT DEFAULT NULL,
                        PRIMARY KEY(`paymentMethodId`),
                        FOREIGN KEY(`paymentMethodId`) REFERENCES `payment_methods`(`id`) ON DELETE CASCADE
                    )
                """,
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `satispay_details` (
                        `paymentMethodId` TEXT NOT NULL,
                        `weeklyBudget` REAL NOT NULL,
                        `sddDay` INTEGER NOT NULL DEFAULT 1,
                        `iban` TEXT DEFAULT NULL,
                        PRIMARY KEY(`paymentMethodId`),
                        FOREIGN KEY(`paymentMethodId`) REFERENCES `payment_methods`(`id`) ON DELETE CASCADE
                    )
                """,
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `paypal_details` (
                        `paymentMethodId` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `bnplInstallmentCount` INTEGER NOT NULL DEFAULT 3,
                        `bnplCycleDays` INTEGER NOT NULL DEFAULT 14,
                        PRIMARY KEY(`paymentMethodId`),
                        FOREIGN KEY(`paymentMethodId`) REFERENCES `payment_methods`(`id`) ON DELETE CASCADE
                    )
                """,
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `klarna_details` (
                        `paymentMethodId` TEXT NOT NULL,
                        `bnplInstallmentCount` INTEGER NOT NULL DEFAULT 4,
                        `bnplCycleDays` INTEGER NOT NULL DEFAULT 30,
                        PRIMARY KEY(`paymentMethodId`),
                        FOREIGN KEY(`paymentMethodId`) REFERENCES `payment_methods`(`id`) ON DELETE CASCADE
                    )
                """,
                )
                db.execSQL(
                    """
                    INSERT INTO `payment_methods` (`id`, `name`, `provider`, `isActive`, `issuer`, `currency`)
                    SELECT `id`, `name`,
                        CASE WHEN `type` = 'SALDO' THEN 'CREDIT_CARD_SALDO' ELSE 'CREDIT_CARD_REVOLVING' END,
                        1, NULL, NULL
                    FROM `credit_cards`
                """,
                )
                db.execSQL(
                    """
                    INSERT INTO `credit_card_details` (`paymentMethodId`, `cardType`, `limit`, `closingDay`, `paymentDay`, `interestRate`, `issuer`)
                    SELECT `id`, `type`, `limit`, `closingDay`, `paymentDay`, NULL, NULL
                    FROM `credit_cards`
                """,
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `paymentMethodId` TEXT DEFAULT NULL")
                db.execSQL("UPDATE `transactions` SET `paymentMethodId` = `creditCardId` WHERE `creditCardId` IS NOT NULL")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    INSERT INTO `credit_card_details` (`paymentMethodId`, `cardType`, `limit`, `closingDay`, `paymentDay`)
                    SELECT pm.`id`,
                        CASE WHEN pm.`provider` = 'CREDIT_CARD_SALDO' THEN 'SALDO' ELSE 'REVOLVING' END,
                        0.0, 0, 0
                    FROM `payment_methods` pm
                    LEFT JOIN `credit_card_details` ccd ON pm.`id` = ccd.`paymentMethodId`
                    WHERE (pm.`provider` = 'CREDIT_CARD_SALDO' OR pm.`provider` = 'CREDIT_CARD_REVOLVING')
                        AND ccd.`paymentMethodId` IS NULL
                """,
                )
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `debit_card_details` (
                        `paymentMethodId` TEXT NOT NULL,
                        `issuer` TEXT DEFAULT NULL,
                        `cardNumber` TEXT DEFAULT NULL,
                        `notes` TEXT DEFAULT NULL,
                        PRIMARY KEY(`paymentMethodId`),
                        FOREIGN KEY(`paymentMethodId`) REFERENCES `payment_methods`(`id`) ON DELETE CASCADE
                    )
                """,
                )
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_description ON transactions(description)")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE transactions SET
                        creditCardId = (
                            SELECT t2.creditCardId FROM transactions t2
                            WHERE t2.groupId = transactions.groupId
                            AND t2.id != transactions.id
                            AND t2.creditCardId IS NOT NULL
                            LIMIT 1
                        ),
                        paymentMethodId = (
                            SELECT t2.paymentMethodId FROM transactions t2
                            WHERE t2.groupId = transactions.groupId
                            AND t2.id != transactions.id
                            AND t2.paymentMethodId IS NOT NULL
                            LIMIT 1
                        )
                    WHERE type = 'income'
                        AND creditCardId IS NULL
                        AND paymentMethodId IS NULL
                        AND groupId IS NOT NULL
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_methods_provider ON payment_methods(provider)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_type ON categories(type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_isCreditCard ON transactions(isCreditCard)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_currency_rates_lastUpdatedTimestamp ON currency_rates(lastUpdatedTimestamp)")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `credit_card_installment_plans` (
                        `id` TEXT NOT NULL,
                        `transactionId` TEXT NOT NULL,
                        `paymentMethodId` TEXT NOT NULL,
                        `totalAmount` REAL NOT NULL,
                        `installmentCount` INTEGER NOT NULL,
                        `installmentAmount` REAL NOT NULL,
                        `paidCount` INTEGER NOT NULL,
                        `startDate` TEXT NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`paymentMethodId`) REFERENCES `payment_methods`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """,
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_credit_card_installment_plans_transactionId` ON `credit_card_installment_plans` (`transactionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_credit_card_installment_plans_paymentMethodId` ON `credit_card_installment_plans` (`paymentMethodId`)")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `credit_card_installment_plans_new` (
                        `id` TEXT NOT NULL,
                        `paymentMethodId` TEXT NOT NULL,
                        `totalAmount` REAL NOT NULL,
                        `installmentCount` INTEGER NOT NULL,
                        `installmentAmount` REAL NOT NULL,
                        `paidCount` INTEGER NOT NULL,
                        `startDate` TEXT NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`paymentMethodId`) REFERENCES `payment_methods`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `credit_card_installment_plans_new` (id, paymentMethodId, totalAmount, installmentCount, installmentAmount, paidCount, startDate)
                    SELECT id, paymentMethodId, totalAmount, installmentCount, installmentAmount, paidCount, startDate FROM `credit_card_installment_plans`
                """,
                )
                db.execSQL("DROP TABLE IF EXISTS `credit_card_installment_plans`")
                db.execSQL("ALTER TABLE `credit_card_installment_plans_new` RENAME TO `credit_card_installment_plans`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_credit_card_installment_plans_paymentMethodId` ON `credit_card_installment_plans` (`paymentMethodId`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `installment_scheduled_payments` (
                        `id` TEXT NOT NULL,
                        `planId` TEXT NOT NULL,
                        `dueDate` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `status` TEXT NOT NULL DEFAULT 'PENDING',
                        `expenseTransactionId` TEXT DEFAULT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`planId`) REFERENCES `credit_card_installment_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_installment_scheduled_payments_planId` ON `installment_scheduled_payments` (`planId`)")
                val cursor = db.query("SELECT id, totalAmount, installmentCount, installmentAmount, paidCount, startDate FROM `credit_card_installment_plans`")
                while (cursor.moveToNext()) {
                    val planId = cursor.getString(0)
                    val totalAmount = cursor.getDouble(1)
                    val installmentCount = cursor.getInt(2)
                    val installmentAmount = cursor.getDouble(3)
                    val paidCount = cursor.getInt(4)
                    val startDate = cursor.getString(5)
                    val parts = startDate.split("-")
                    val baseYear = parts[0].toIntOrNull() ?: 2024
                    val baseMonth = parts[1].toIntOrNull() ?: 1
                    val baseDay = (parts[2].toIntOrNull() ?: 1).coerceIn(1, 28)
                    for (i in paidCount until installmentCount) {
                        val paymentId = "migrated_${planId}_$i"
                        val monthOffset = i - paidCount
                        var year = baseYear + (baseMonth - 1 + monthOffset) / 12
                        var month = ((baseMonth - 1 + monthOffset) % 12) + 1
                        val day = baseDay.coerceAtMost(28)
                        val ym = "%04d-%02d".format(year, month)
                        val dueDate = "$ym-${"%02d".format(day)}"
                        val remaining = totalAmount - (installmentAmount * (installmentCount - 1))
                        val amount = if (i == installmentCount - 1 && remaining != installmentAmount) {
                            remaining
                        } else {
                            installmentAmount
                        }
                        db.execSQL("INSERT INTO `installment_scheduled_payments` (`id`, `planId`, `dueDate`, `amount`, `status`) VALUES ('$paymentId', '$planId', '$dueDate', $amount, 'PENDING')")
                    }
                }
                cursor.close()
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `amex_statements` (
                        `id` TEXT NOT NULL,
                        `paymentMethodId` TEXT NOT NULL,
                        `statementMonth` TEXT NOT NULL,
                        `totalExpenses` REAL NOT NULL DEFAULT 0,
                        `totalPagoflex` REAL NOT NULL DEFAULT 0,
                        `revolvingBalance` REAL NOT NULL DEFAULT 0,
                        `paymentMode` TEXT NOT NULL,
                        `paymentAmount` REAL NOT NULL DEFAULT 0,
                        `closingDate` TEXT NOT NULL,
                        `paymentDueDate` TEXT NOT NULL,
                        `isClosed` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`paymentMethodId`) REFERENCES `payment_methods`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_amex_statements_paymentMethodId_statementMonth` ON `amex_statements` (`paymentMethodId`, `statementMonth`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `amex_pagoflex_plans` (
                        `id` TEXT NOT NULL,
                        `statementId` TEXT NOT NULL,
                        `transactionId` TEXT NOT NULL,
                        `totalAmount` REAL NOT NULL,
                        `installmentCount` INTEGER NOT NULL,
                        `installmentAmount` REAL NOT NULL,
                        `paidCount` INTEGER NOT NULL,
                        `startDate` TEXT NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`statementId`) REFERENCES `amex_statements`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_amex_pagoflex_plans_statementId` ON `amex_pagoflex_plans` (`statementId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_amex_pagoflex_plans_transactionId` ON `amex_pagoflex_plans` (`transactionId`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `amex_revolving_balances` (
                        `id` TEXT NOT NULL,
                        `statementId` TEXT NOT NULL,
                        `carriedForwardDebt` REAL NOT NULL,
                        `interestCharged` REAL NOT NULL DEFAULT 0,
                        `interestRate` REAL NOT NULL DEFAULT 0,
                        `userPaymentChoice` TEXT NOT NULL,
                        `paymentAmount` REAL NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`statementId`) REFERENCES `amex_statements`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_amex_revolving_balances_statementId` ON `amex_revolving_balances` (`statementId`)")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `amex_pagoflex_scheduled_payments` (
                        `id` TEXT NOT NULL,
                        `planId` TEXT NOT NULL,
                        `sequenceNumber` INTEGER NOT NULL,
                        `dueDate` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `status` TEXT NOT NULL DEFAULT 'PENDING',
                        `expenseTransactionId` TEXT DEFAULT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`planId`) REFERENCES `amex_pagoflex_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_amex_pagoflex_scheduled_payments_planId` ON `amex_pagoflex_scheduled_payments` (`planId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_amex_pagoflex_scheduled_payments_dueDate` ON `amex_pagoflex_scheduled_payments` (`dueDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_amex_pagoflex_scheduled_payments_status` ON `amex_pagoflex_scheduled_payments` (`status`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `amex_pagoflex_plan_changes` (
                        `id` TEXT NOT NULL,
                        `planId` TEXT NOT NULL,
                        `changedAt` TEXT NOT NULL,
                        `previousInstallmentCount` INTEGER NOT NULL,
                        `newInstallmentCount` INTEGER NOT NULL,
                        `previousInstallmentAmount` REAL NOT NULL,
                        `newInstallmentAmount` REAL NOT NULL,
                        `remainingDebtAtChange` REAL NOT NULL,
                        `reason` TEXT DEFAULT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`planId`) REFERENCES `amex_pagoflex_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_amex_pagoflex_plan_changes_planId` ON `amex_pagoflex_plan_changes` (`planId`)")

                db.execSQL("ALTER TABLE `amex_pagoflex_plans` ADD COLUMN `planType` TEXT NOT NULL DEFAULT 'FIXED_DURATION'")
                db.execSQL("ALTER TABLE `amex_pagoflex_plans` ADD COLUMN `initialInstallmentAmount` REAL DEFAULT NULL")

                db.query("SELECT id, totalAmount, installmentCount, installmentAmount, paidCount, startDate FROM `amex_pagoflex_plans`").use { cursor ->
                    while (cursor.moveToNext()) {
                        val planId = cursor.getString(0)
                        val totalAmount = cursor.getDouble(1)
                        val installmentCount = cursor.getInt(2)
                        val installmentAmount = cursor.getDouble(3)
                        val paidCount = cursor.getInt(4)
                        val startDate = cursor.getString(5)
                        val parts = startDate.split("-")
                        val baseYear = parts.getOrNull(0)?.toIntOrNull() ?: 2024
                        val baseMonth = parts.getOrNull(1)?.toIntOrNull() ?: 1
                        val baseDay = (parts.getOrNull(2)?.toIntOrNull() ?: 1).coerceIn(1, 28)

                        for (i in 0 until installmentCount) {
                            val paymentId = "migrated_${planId}_$i" + UUID.randomUUID().toString()
                            val monthOffset = i
                            val year = baseYear + (baseMonth - 1 + monthOffset) / 12
                            val month = ((baseMonth - 1 + monthOffset) % 12) + 1
                            val day = baseDay.coerceAtMost(28)
                            val dueDate = "%04d-%02d-%02d".format(year, month, day)
                            val status = if (i < paidCount) "PAID" else "PENDING"
                            val amount = if (i == installmentCount - 1) {
                                val regularTotal = installmentAmount * (installmentCount - 1)
                                (totalAmount - regularTotal).coerceAtLeast(0.0)
                            } else {
                                installmentAmount
                            }
                            db.execSQL(
                                "INSERT INTO `amex_pagoflex_scheduled_payments` (" +
                                    "`id`, `planId`, `sequenceNumber`, `dueDate`, `amount`, `status`) " +
                                    "VALUES (?, ?, ?, ?, ?, ?)",
                                arrayOf(paymentId, planId, i, dueDate, amount, status),
                            )
                        }
                    }
                }
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `credit_card_details` ADD COLUMN `linkedPaymentMethodId` TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `payment_methods` (`id`, `name`, `provider`, `isActive`, `issuer`, `currency`)
                    SELECT cc.`id`, cc.`name`,
                        CASE WHEN cc.`type` = 'SALDO' THEN 'CREDIT_CARD_SALDO' ELSE 'CREDIT_CARD_REVOLVING' END,
                        1, NULL, NULL
                    FROM `credit_cards` cc
                    LEFT JOIN `payment_methods` pm ON cc.`id` = pm.`id`
                    WHERE pm.`id` IS NULL
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `credit_card_details` (`paymentMethodId`, `cardType`, `limit`, `closingDay`, `paymentDay`)
                    SELECT cc.`id`, cc.`type`, cc.`limit`, cc.`closingDay`, cc.`paymentDay`
                    FROM `credit_cards` cc
                    LEFT JOIN `credit_card_details` ccd ON cc.`id` = ccd.`paymentMethodId`
                    WHERE ccd.`paymentMethodId` IS NULL
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE IF EXISTS `credit_cards`")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `imageUri` TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Crea la nuova tabella credit_cards
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `credit_cards` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `limit` REAL NOT NULL,
                        `closingDay` INTEGER NOT NULL,
                        `paymentDay` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """,
                )

                // 2. Aggiungi la colonna creditCardId alla tabella transactions
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `creditCardId` TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Aggiungi colonne per la ricorrenza
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `recurrenceType` TEXT NOT NULL DEFAULT 'none'")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `recurrenceEndDate` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `recurrenceLimit` INTEGER DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                val builder = Room
                    .databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "spese_db_v6",
                    )
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22)
                if ((context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                    builder.fallbackToDestructiveMigration(dropAllTables = true)
                }
                builder
                    .build()
                    .also { instance = it }
            }
    }
}
