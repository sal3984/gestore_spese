package com.alessandrogregorio.gestorespese.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 3. Il Database
@Database(entities = [TransactionEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spese_db_v2"
                )
                    .fallbackToDestructiveMigration() // Necessario per l'aggiornamento (cancella i vecchi dati!)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
