package com.expense.management.data

data class ActiveCreditCardEntity(
    val paymentMethod: PaymentMethodEntity,
    val creditCardDetail: CreditCardDetailEntity,
)
