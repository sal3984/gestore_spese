package com.expense.management.data

import com.google.gson.annotations.SerializedName

enum class TransactionType(val value: String) {
    @SerializedName("income")
    INCOME("income"),
    @SerializedName("expense")
    EXPENSE("expense")
}
