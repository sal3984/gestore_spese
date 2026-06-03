package com.expense.management.domain.model

sealed class PaymentMethodDetails {
    data class CreditCard(
        val name: String,
        val cardType: CreditCardType,
        val limit: Double,
        val closingDay: Int,
        val paymentDay: Int,
    ) : PaymentMethodDetails()

    data class DebitCard(
        val name: String,
        val issuer: String?,
        val cardNumber: String?,
        val notes: String?,
    ) : PaymentMethodDetails()

    data class Revolut(
        val name: String,
        val currency: String,
        val iban: String?,
        val accountNumber: String?,
    ) : PaymentMethodDetails()

    data class Satispay(
        val name: String,
        val weeklyBudget: Double,
        val sddDay: Int,
        val iban: String?,
    ) : PaymentMethodDetails()

    data class Paypal(
        val name: String,
        val email: String,
        val bnplInstallmentCount: Int,
        val bnplCycleDays: Int,
    ) : PaymentMethodDetails()

    data class Klarna(
        val name: String,
        val bnplInstallmentCount: Int,
        val bnplCycleDays: Int,
    ) : PaymentMethodDetails()

    data class Cash(
        val name: String,
    ) : PaymentMethodDetails()
}
