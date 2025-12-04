package com.expense.management.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TransactionEntity::class, CategoryEntity::class], version = 5, exportSchema = false)
@TypeConverters(TransactionTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                Room
                    .databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "spese_db_v5",
                    )
                    .build()
                    .also { instance = it }
            }
    }
}
