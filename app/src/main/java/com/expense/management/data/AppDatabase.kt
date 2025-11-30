package com.expense.management.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 3. Il Database
@Database(entities = [TransactionEntity::class, CategoryEntity::class], version = 5, exportSchema = false) // Versione aggiornata per migrazione schema
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    // Aggiornato a v5 per reset e applicazione nuovi campi rateali
                    "spese_db_v5",
                )
                    // Cancella i dati vecchi al cambio versione (Ok per prototipazione)
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
        }
    }
}
