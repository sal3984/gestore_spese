package com.expense.management.domain.usecase

import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.PaymentProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.YearMonth

@DisplayName("CalculateCreditCardSummariesUseCase")
class CalculateCreditCardSummariesUseCaseTest {

    private val useCase = CalculateCreditCardSummariesUseCase()
    private val month = YearMonth.of(2026, 10)

    private fun card(
        id: String,
        cardType: CreditCardType,
        limit: Double = 1000.0,
        closingDay: Int = 20,
        paymentDay: Int = 15,
    ) = ActiveCreditCard(
        id = id,
        name = "Card $id",
        provider = PaymentProvider.CREDIT_CARD_SALDO,
        cardType = cardType,
        limit = limit,
        closingDay = closingDay,
        paymentDay = paymentDay,
    )

    private fun transaction(
        id: String,
        amount: Double,
        type: TransactionType,
        isCreditCard: Boolean,
        effectiveDate: String,
        creditCardId: String? = null,
        paymentMethodId: String? = null,
    ) = TransactionEntity(
        id = id,
        date = effectiveDate,
        description = id,
        amount = amount,
        categoryId = "shopping",
        type = type,
        isCreditCard = isCreditCard,
        effectiveDate = effectiveDate,
        originalAmount = amount,
        originalCurrency = "\u20ac",
        creditCardId = creditCardId,
        paymentMethodId = paymentMethodId,
    )

    @Test
    fun `saldo card with a cycle shows its unsettled exposure whatever the month`() {
        val saldo = card("saldo1", CreditCardType.SALDO, limit = 2000.0)
        val transactions = listOf(
            transaction("t1", 100.0, TransactionType.EXPENSE, true, "2026-10-15", creditCardId = "saldo1"),
            transaction("t3", 999.0, TransactionType.EXPENSE, true, "2026-11-15", creditCardId = "saldo1"),
        )

        val october = useCase.execute(transactions, listOf(saldo), month, emptyMap(), mapOf("saldo1" to 350.0))["saldo1"]!!
        val november = useCase.execute(transactions, listOf(saldo), month.plusMonths(1), emptyMap(), mapOf("saldo1" to 350.0))["saldo1"]!!

        assertEquals(350.0, october.displayedSpent, 0.01)
        assertEquals(350.0, october.totalUtilized, 0.01)
        assertEquals(0.0, october.totalPaid, 0.01)
        assertEquals(0.175f, october.progress, 0.001f)
        assertEquals(october, november)
    }

    @Test
    fun `saldo card with a cycle and nothing left to charge shows an empty bar`() {
        val saldo = card("saldo1", CreditCardType.SALDO)
        val transactions = listOf(transaction("t1", 100.0, TransactionType.EXPENSE, true, "2026-10-15", creditCardId = "saldo1"))

        val summary = useCase.execute(transactions, listOf(saldo), month, emptyMap(), emptyMap())["saldo1"]!!

        assertEquals(0.0, summary.displayedSpent, 0.01)
        assertEquals(0.0f, summary.progress, 0.001f)
    }

    @Test
    fun `saldo card without cycle days keeps the month spending, non credit card expenses included`() {
        val saldo = card("saldo1", CreditCardType.SALDO, closingDay = 0, paymentDay = 0)
        val transactions = listOf(
            transaction("t1", 80.0, TransactionType.EXPENSE, false, "2026-10-10", paymentMethodId = "saldo1"),
        )

        val summary = useCase.execute(transactions, listOf(saldo), month, emptyMap())["saldo1"]!!

        assertEquals(80.0, summary.displayedSpent, 0.01)
    }

    @Test
    fun `revolving card nets repayments against usage across every month`() {
        val revolving = card("rev1", CreditCardType.REVOLVING, limit = 1000.0)
        val transactions = listOf(
            transaction("t1", 600.0, TransactionType.EXPENSE, true, "2026-08-10", creditCardId = "rev1"),
            transaction("t2", 400.0, TransactionType.EXPENSE, true, "2026-10-10", creditCardId = "rev1"),
            transaction("t3", 300.0, TransactionType.INCOME, true, "2026-09-10", creditCardId = "rev1"),
        )

        val summary = useCase.execute(transactions, listOf(revolving), month, emptyMap())["rev1"]!!

        assertEquals(1000.0, summary.totalUtilized, 0.01)
        assertEquals(300.0, summary.totalRepaid, 0.01)
        assertEquals(700.0, summary.displayedSpent, 0.01)
        assertEquals(0.7f, summary.progress, 0.001f)
    }

    @Test
    fun `revolving card can report a negative balance when repayments exceed usage`() {
        val revolving = card("rev1", CreditCardType.REVOLVING, limit = 1000.0)
        val transactions = listOf(
            transaction("t1", 100.0, TransactionType.EXPENSE, true, "2026-10-10", creditCardId = "rev1"),
            transaction("t2", 250.0, TransactionType.INCOME, true, "2026-10-11", creditCardId = "rev1"),
        )

        val summary = useCase.execute(transactions, listOf(revolving), month, emptyMap())["rev1"]!!

        assertEquals(-150.0, summary.displayedSpent, 0.01)
    }

    @Test
    fun `amex card counts paid pagoflex installments as repayments and never goes below zero`() {
        val amex = card("amex1", CreditCardType.AMEX_HYBRID, limit = 5000.0)
        val transactions = listOf(
            transaction("t1", 600.0, TransactionType.EXPENSE, true, "2026-10-10", creditCardId = "amex1"),
            transaction("t2", 100.0, TransactionType.INCOME, true, "2026-10-11", creditCardId = "amex1"),
        )

        val summary = useCase.execute(transactions, listOf(amex), month, mapOf("amex1" to 900.0))["amex1"]!!

        assertEquals(600.0, summary.totalUtilized, 0.01)
        assertEquals(1000.0, summary.totalRepaid, 0.01)
        assertEquals(0.0, summary.displayedSpent, 0.01)
        assertEquals(0.0f, summary.progress, 0.001f)
    }

    @Test
    fun `progress is zero when the card has no limit`() {
        val saldo = card("saldo1", CreditCardType.SALDO, limit = 0.0)
        val transactions = listOf(
            transaction("t1", 100.0, TransactionType.EXPENSE, true, "2026-10-10", creditCardId = "saldo1"),
        )

        val summary = useCase.execute(transactions, listOf(saldo), month, emptyMap(), mapOf("saldo1" to 100.0))["saldo1"]!!

        assertEquals(100.0, summary.displayedSpent, 0.01)
        assertEquals(0.0f, summary.progress, 0.001f)
    }
}
