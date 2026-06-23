package com.expense.management.domain.model

data class CreditCardDetails(
    val paymentMethodId: String,
    val cardType: CreditCardType,
    val limit: Double,
    val closingDay: Int = 0,
    val paymentDay: Int = 0,
    val interestRate: Double? = null,
    val issuer: String? = null,
    val linkedPaymentMethodId: String? = null,
)
