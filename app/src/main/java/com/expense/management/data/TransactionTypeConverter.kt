package com.expense.management.data

import androidx.room.TypeConverter
import com.expense.management.domain.model.PaymentProvider

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
                else -> null
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

    @TypeConverter
    fun fromCardType(cardType: CardType?): String? {
        return cardType?.name
    }

    @TypeConverter
    fun toCardType(value: String?): CardType? {
        return value?.let {
            try {
                CardType.valueOf(it)
            } catch (_: Exception) {
                null
            }
        }
    }

    @TypeConverter
    fun fromPaymentProvider(provider: PaymentProvider?): String? {
        return provider?.name
    }

    @TypeConverter
    fun toPaymentProvider(value: String?): PaymentProvider? {
        return value?.let { PaymentProvider.safeValueOf(it) }
    }
}
