package com.expense.management.data

data class BackupData(
    val transactions: List<TransactionEntity>?,
    val categories: List<CategoryEntity>?,
    val creditCard: List<CreditCardEntity>?,
    val paymentMethods: List<PaymentMethodEntity>? = null,
    val creditCardDetails: List<CreditCardDetailEntity>? = null,
    val revolutDetails: List<RevolutDetailEntity>? = null,
    val satispayDetails: List<SatispayDetailEntity>? = null,
    val paypalDetails: List<PaypalDetailEntity>? = null,
    val klarnaDetails: List<KlarnaDetailEntity>? = null,
    val debitCardDetails: List<DebitCardDetailEntity>? = null,
)
