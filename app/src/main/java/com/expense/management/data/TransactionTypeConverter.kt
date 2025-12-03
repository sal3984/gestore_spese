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
}
