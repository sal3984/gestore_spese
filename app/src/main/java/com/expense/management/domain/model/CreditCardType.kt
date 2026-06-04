package com.expense.management.domain.model

enum class CreditCardType {
    SALDO,
    REVOLVING,
    ;

    companion object {
        fun safeValueOf(name: String): CreditCardType? =
            entries.find { it.name == name }
    }
}
