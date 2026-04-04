package com.expense.management.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        CurrencyRate::class,
        CreditCardEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
@TypeConverters(TransactionTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    abstract fun categoryDao(): CategoryDao

    abstract fun currencyDao(): CurrencyDao

    abstract fun creditCardDao(): CreditCardDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

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
                Room
                    .databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "spese_db_v6",
                    )
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
