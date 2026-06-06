package com.expense.management.domain.model

enum class AmexPaymentMode {
    SALDO,
    MINIMUM,
    FIXED,
    PAGOFLEX_ONLY,
    ;

    companion object {
        fun safeValueOf(name: String): AmexPaymentMode? =
            entries.find { it.name == name }
    }
}
