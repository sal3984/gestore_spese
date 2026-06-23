package com.expense.management.domain.model

enum class RecurrenceType(val value: String) {
    NONE("none"),
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly"),
    YEARLY("yearly"),
}
