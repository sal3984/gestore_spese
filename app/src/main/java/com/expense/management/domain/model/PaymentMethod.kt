package com.expense.management.domain.model

sealed class PaymentMethod {
    abstract val id: String
    abstract val name: String
    abstract val provider: PaymentProvider
    abstract val isActive: Boolean
    abstract val issuer: String?
    abstract val currency: String?

    data class CreditCard(
        override val id: String,
        override val name: String,
        override val isActive: Boolean = true,
        override val issuer: String? = null,
        override val currency: String? = null,
        val cardType: CreditCardType,
        val limit: Double,
        val closingDay: Int = 0,
        val paymentDay: Int = 0,
        val interestRate: Double? = null,
    ) : PaymentMethod() {
        override val provider: PaymentProvider
            get() = when (cardType) {
                CreditCardType.SALDO -> PaymentProvider.CREDIT_CARD_SALDO
                CreditCardType.REVOLVING -> PaymentProvider.CREDIT_CARD_REVOLVING
                CreditCardType.INSTALLMENT -> PaymentProvider.CREDIT_CARD_INSTALLMENT
                CreditCardType.AMEX_HYBRID -> PaymentProvider.CREDIT_CARD_AMEX
            }
    }

    data class Revolut(
        override val id: String,
        override val name: String,
        override val isActive: Boolean = true,
        override val issuer: String? = null,
        override val currency: String? = "EUR",
        val iban: String? = null,
        val accountNumber: String? = null,
    ) : PaymentMethod() {
        override val provider: PaymentProvider
            get() = PaymentProvider.REVOLUT
    }

    data class Satispay(
        override val id: String,
        override val name: String,
        override val isActive: Boolean = true,
        override val issuer: String? = null,
        override val currency: String? = "EUR",
        val weeklyBudget: Double,
        val sddDay: Int = 1,
        val iban: String? = null,
    ) : PaymentMethod() {
        override val provider: PaymentProvider
            get() = PaymentProvider.SATISPAY
    }

    data class PayPal(
        override val id: String,
        override val name: String,
        override val isActive: Boolean = true,
        override val issuer: String? = null,
        override val currency: String? = null,
        val email: String,
        val bnplInstallmentCount: Int = 3,
        val bnplCycleDays: Int = 14,
    ) : PaymentMethod() {
        override val provider: PaymentProvider
            get() = PaymentProvider.PAYPAL
    }

    data class Klarna(
        override val id: String,
        override val name: String,
        override val isActive: Boolean = true,
        override val issuer: String? = null,
        override val currency: String? = null,
        val bnplInstallmentCount: Int = 4,
        val bnplCycleDays: Int = 14,
    ) : PaymentMethod() {
        override val provider: PaymentProvider
            get() = PaymentProvider.KLARNA
    }
}
