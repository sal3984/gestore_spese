package com.expense.management.data

import androidx.room.TypeConverter

class TransactionTypeConverter {

    @TypeConverter
    fun fromTransactionType(transactionType: TransactionType?): String? {
        return transactionType?.value
    }

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? {
        return value?.let {
            when (it) {
                "income" -> TransactionType.INCOME
                "expense" -> TransactionType.EXPENSE
                else -> throw IllegalArgumentException("Unknown transaction type value: $it")
            }
        }
    }

    @TypeConverter
    fun fromRecurrenceType(recurrenceType: RecurrenceType?): String? {
        return recurrenceType?.value
    }

    @TypeConverter
    fun toRecurrenceType(value: String?): RecurrenceType? {
        return value?.let {
            when (it) {
                "none" -> RecurrenceType.NONE
                "daily" -> RecurrenceType.DAILY
                "weekly" -> RecurrenceType.WEEKLY
                "monthly" -> RecurrenceType.MONTHLY
                "yearly" -> RecurrenceType.YEARLY
                else -> RecurrenceType.NONE
            }
        }
    }
}
