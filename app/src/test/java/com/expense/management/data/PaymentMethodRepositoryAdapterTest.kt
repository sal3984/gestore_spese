package com.expense.management.data

import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.PaymentMethod
import com.expense.management.domain.model.PaymentMethodDetails
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PaymentMethodRepositoryAdapter")
class PaymentMethodRepositoryAdapterTest {

    private lateinit var repository: ExpenseRepository
    private lateinit var adapter: PaymentMethodRepositoryAdapter

    private val card = PaymentMethod.CreditCard(
        id = "card1",
        name = "Amex",
        cardType = CreditCardType.AMEX_HYBRID,
        limit = 5000.0,
        closingDay = 20,
        paymentDay = 15,
    )

    private val details = PaymentMethodDetails.CreditCard(
        name = "Amex",
        cardType = CreditCardType.AMEX_HYBRID,
        limit = 6000.0,
        closingDay = 20,
        paymentDay = 15,
        linkedPaymentMethodId = "checking1",
    )

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        adapter = PaymentMethodRepositoryAdapter(repository)
    }

    @Test
    fun `updating a card keeps the columns the domain model does not carry`() {
        coEvery { repository.getCreditCardDetail("card1") } returns CreditCardDetailEntity(
            paymentMethodId = "card1",
            cardType = "AMEX_HYBRID",
            limit = 5000.0,
            closingDay = 20,
            paymentDay = 15,
            interestRate = 14.9,
            issuer = "American Express",
            linkedPaymentMethodId = "checking1",
        )
        val written = slot<CreditCardDetailEntity>()
        coEvery { repository.insertCreditCardDetail(capture(written)) } returns Unit

        runBlocking { adapter.updatePaymentMethod(card, details) }

        assertEquals(14.9, written.captured.interestRate!!, 0.001)
        assertEquals("American Express", written.captured.issuer)
        assertEquals(6000.0, written.captured.limit, 0.001)
        assertEquals("checking1", written.captured.linkedPaymentMethodId)
    }

    @Test
    fun `saving a brand new card leaves the unmapped columns empty`() {
        coEvery { repository.getCreditCardDetail("card1") } returns null
        val written = slot<CreditCardDetailEntity>()
        coEvery { repository.insertCreditCardDetail(capture(written)) } returns Unit

        runBlocking { adapter.savePaymentMethod(card, details) }

        assertNull(written.captured.interestRate)
        assertNull(written.captured.issuer)
        assertEquals("AMEX_HYBRID", written.captured.cardType)
    }
}
