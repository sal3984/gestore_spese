package com.expense.management.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "currency_rates",
    indices = [Index(value = ["lastUpdatedTimestamp"])],
)
data class CurrencyRate(
    @PrimaryKey val currencyCode: String,
    val rateAgainstEuro: Double,
    val lastUpdatedTimestamp: Long,
)
