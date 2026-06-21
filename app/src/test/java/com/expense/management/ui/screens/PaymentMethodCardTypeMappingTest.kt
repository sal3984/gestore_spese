package com.expense.management.ui.screens

import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.PaymentProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PaymentMethodCardTypeMappingTest {

    @Test
    fun `credit card saldo maps to saldo type`() {
        assertEquals(CreditCardType.SALDO, cardTypeForProvider(PaymentProvider.CREDIT_CARD_SALDO))
    }

    @Test
    fun `credit card revolving maps to revolving type`() {
        assertEquals(CreditCardType.REVOLVING, cardTypeForProvider(PaymentProvider.CREDIT_CARD_REVOLVING))
    }

    @Test
    fun `credit card installment maps to installment type`() {
        assertEquals(CreditCardType.INSTALLMENT, cardTypeForProvider(PaymentProvider.CREDIT_CARD_INSTALLMENT))
    }

    @Test
    fun `credit card amex maps to amex hybrid type`() {
        assertEquals(CreditCardType.AMEX_HYBRID, cardTypeForProvider(PaymentProvider.CREDIT_CARD_AMEX))
    }

    @Test
    fun `non credit card provider defaults to saldo type`() {
        assertEquals(CreditCardType.SALDO, cardTypeForProvider(PaymentProvider.CASH))
    }
}
