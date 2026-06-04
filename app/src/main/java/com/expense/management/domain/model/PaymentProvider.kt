package com.expense.management.domain.model

enum class PaymentProvider {
    CASH,
    CREDIT_CARD_SALDO,
    CREDIT_CARD_REVOLVING,
    DEBIT_CARD,
    REVOLUT,
    SATISPAY,
    PAYPAL,
    KLARNA,
    ;

    companion object {
        fun safeValueOf(name: String): PaymentProvider? =
            entries.find { it.name == name }
    }
}
