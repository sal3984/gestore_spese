package com.expense.management.domain.model

data class ActiveCreditCard(
    val id: String,
    val name: String,
    val provider: PaymentProvider,
    val cardType: CreditCardType,
    val limit: Double,
    val closingDay: Int,
    val paymentDay: Int,
    val linkedPaymentMethodId: String? = null,
)
