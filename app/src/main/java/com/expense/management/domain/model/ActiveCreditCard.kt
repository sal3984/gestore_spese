package com.expense.management.domain.model

/**
 * Domain model for an active credit card payment method,
 * combining PaymentMethodEntity with CreditCardDetailEntity.
 * Used in AddTransactionScreen and related flows.
 */
data class ActiveCreditCard(
    val id: String,
    val name: String,
    val provider: PaymentProvider,
    val cardType: CreditCardType,
    val limit: Double,
    val closingDay: Int,
    val paymentDay: Int,
)
