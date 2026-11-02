package com.expense.management.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ActiveCreditCardTest {

    @ParameterizedTest(name = "{0} chiusura {1} addebito {2} -> {3}")
    @CsvSource(
        "SALDO, 30, 15, true",
        "SALDO, 15, 15, true",
        "SALDO, 0, 15, false",
        "SALDO, 30, 0, false",
        "REVOLVING, 30, 15, false",
        "INSTALLMENT, 30, 15, false",
    )
    fun `solo la carta a saldo con giorni validi passa dal ciclo`(
        type: CreditCardType,
        closingDay: Int,
        paymentDay: Int,
        expected: Boolean,
    ) {
        val card = ActiveCreditCard(
            id = "c",
            name = "Carta",
            provider = PaymentProvider.CREDIT_CARD_SALDO,
            cardType = type,
            limit = 1000.0,
            closingDay = closingDay,
            paymentDay = paymentDay,
        )

        assertEquals(expected, card.settlesThroughCycles)
    }
}
