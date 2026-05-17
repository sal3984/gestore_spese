package com.expense.management.data

import com.google.gson.annotations.SerializedName

enum class RecurrenceType(val value: String) {
    @SerializedName("none")
    NONE("none"),

    @SerializedName("daily")
    DAILY("daily"),

    @SerializedName("weekly")
    WEEKLY("weekly"),

    @SerializedName("monthly")
    MONTHLY("monthly"),

    @SerializedName("yearly")
    YEARLY("yearly"),
}
