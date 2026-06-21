package com.expense.management.domain.usecase

import com.expense.management.data.CategoryEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.PaymentProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("GenerateCreditCardPaymentUseCase")
class GenerateCreditCardPaymentUseCaseTest {

    private lateinit var useCase: GenerateCreditCardPaymentUseCase
    private val expenseCategory = CategoryEntity(id = "credit_card_payment", label = "CC Payment", icon = "💳", type = TransactionType.EXPENSE)
    private val incomeCategory = CategoryEntity(id = "credit_card_adjustment", label = "CC Adj", icon = "🔄", type = TransactionType.INCOME)
    private val categories = listOf(expenseCategory, incomeCategory)
    private val paymentDate = LocalDate.of(2024, 6, 25)

    @BeforeEach
    fun setUp() {
        useCase = GenerateCreditCardPaymentUseCase()
    }

    @Test
    fun `should generate bank debit and credit restoration for SALDO card`() {
        val card = ActiveCreditCard(
            id = "card_1",
            name = "Visa Gold",
            provider = PaymentProvider.CREDIT_CARD_SALDO,
            cardType = CreditCardType.SALDO,
            limit = 5000.0,
            closingDay = 15,
            paymentDay = 25,
        )
        val result = useCase.execute(card, 1000.0, paymentDate, categories)
        assertEquals(2, result.size)

        val debitTx = result.first()
        assertEquals(1000.0, debitTx.amount)
        assertEquals(TransactionType.EXPENSE, debitTx.type)
        assertEquals("credit_card_payment", debitTx.categoryId)
        assertTrue(debitTx.description.contains("Addebito"))
        assertTrue(debitTx.description.contains("Visa Gold"))
        assertEquals(paymentDate.toString(), debitTx.date)
        assertEquals(paymentDate.toString(), debitTx.effectiveDate)

        val restoreTx = result[1]
        assertEquals(1000.0, restoreTx.amount)
        assertEquals(TransactionType.INCOME, restoreTx.type)
        assertEquals("credit_card_adjustment", restoreTx.categoryId)
        assertTrue(restoreTx.description.contains("Ripristino plafond"))
        assertTrue(restoreTx.isCreditCard)
        assertEquals("card_1", restoreTx.creditCardId)

        assertEquals(debitTx.groupId, restoreTx.groupId)
        assertEquals("${debitTx.id}_restore", restoreTx.id)
    }

    @Test
    fun `should use Rata label for REVOLVING card`() {
        val card = ActiveCreditCard(
            id = "card_2",
            name = "Revolving",
            provider = PaymentProvider.CREDIT_CARD_REVOLVING,
            cardType = CreditCardType.REVOLVING,
            limit = 3000.0,
            closingDay = 10,
            paymentDay = 5,
        )
        val result = useCase.execute(card, 500.0, paymentDate, categories)
        assertEquals(2, result.size)
        assertTrue(result.first().description.contains("Rata"))
    }

    @Test
    fun `should use Rata label for INSTALLMENT card`() {
        val card = ActiveCreditCard(
            id = "card_3",
            name = "Installment Card",
            provider = PaymentProvider.CREDIT_CARD_INSTALLMENT,
            cardType = CreditCardType.INSTALLMENT,
            limit = 2000.0,
            closingDay = 5,
            paymentDay = 20,
        )
        val result = useCase.execute(card, 250.0, paymentDate, categories)
        assertEquals(2, result.size)
        assertTrue(result.first().description.contains("Rata"))
    }

    @Test
    fun `should fallback to other categories when specific not found`() {
        val fallbackCategories = listOf(
            CategoryEntity(id = "generic_expense", label = "Expense", icon = "📦", type = TransactionType.EXPENSE),
            CategoryEntity(id = "generic_income", label = "Income", icon = "💰", type = TransactionType.INCOME),
        )
        val card = ActiveCreditCard(
            id = "card_1",
            name = "Test",
            provider = PaymentProvider.CREDIT_CARD_SALDO,
            cardType = CreditCardType.SALDO,
            limit = 1000.0,
            closingDay = 15,
            paymentDay = 25,
        )
        val result = useCase.execute(card, 100.0, paymentDate, fallbackCategories)
        assertEquals(2, result.size)
        assertEquals("generic_expense", result.first().categoryId)
        assertEquals("generic_income", result[1].categoryId)
    }

    @Test
    fun `should generate transactions with correct group linkage`() {
        val card = ActiveCreditCard(
            id = "card_1",
            name = "Visa",
            provider = PaymentProvider.CREDIT_CARD_SALDO,
            cardType = CreditCardType.SALDO,
            limit = 5000.0,
            closingDay = 15,
            paymentDay = 25,
        )
        val result = useCase.execute(card, 750.0, paymentDate, categories)
        assertNotNull(result.first().groupId)
        assertEquals(result.first().groupId, result[1].groupId)
    }
}
