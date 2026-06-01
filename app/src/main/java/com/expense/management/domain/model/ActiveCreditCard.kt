package com.expense.management.domain.model

import androidx.compose.runtime.Immutable

/**
 * Domain model for an active credit card payment method,
 * combining PaymentMethodEntity with CreditCardDetailEntity.
 * Used in AddCreditCardTransactionScreen and related flows.
 */
@Immutable
data class ActiveCreditCard(
    val id: String,
    val name: String,
    val provider: PaymentProvider,
    val cardType: CreditCardType,
    val limit: Double,
    val closingDay: Int,
    val paymentDay: Int,
)
