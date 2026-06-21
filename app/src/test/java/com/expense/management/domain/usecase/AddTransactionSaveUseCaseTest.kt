package com.expense.management.domain.usecase

import com.expense.management.data.CategoryEntity
import com.expense.management.data.RecurrenceType
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.PaymentProvider
import com.expense.management.ui.screens.AddTransactionUiState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@DisplayName("AddTransactionSaveUseCase")
class AddTransactionSaveUseCaseTest {

    private lateinit var useCase: AddTransactionSaveUseCase
    private val dateFormat = "dd/MM/yyyy"
    private val locale = Locale.ITALY
    private val today = LocalDate.now()
    private val todayStr = today.format(DateTimeFormatter.ofPattern(dateFormat))
    private val expenseCategory = CategoryEntity(id = "food", label = "Food", icon = "🍕", type = TransactionType.EXPENSE)
    private val incomeCategory = CategoryEntity(id = "salary", label = "Salary", icon = "💰", type = TransactionType.INCOME)
    private val categories = listOf(
        expenseCategory,
        incomeCategory,
        CategoryEntity(id = "credit_card_payment", label = "CC Payment", icon = "💳", type = TransactionType.EXPENSE),
        CategoryEntity(id = "credit_card_adjustment", label = "CC Adjustment", icon = "🔄", type = TransactionType.INCOME),
    )
    private val creditCard = ActiveCreditCard(
        id = "card_1",
        name = "Test Card",
        provider = PaymentProvider.CREDIT_CARD_SALDO,
        cardType = CreditCardType.SALDO,
        limit = 5000.0,
        closingDay = 15,
        paymentDay = 25,
    )
    private val revolvingCard = ActiveCreditCard(
        id = "card_2",
        name = "Revolving Card",
        provider = PaymentProvider.CREDIT_CARD_REVOLVING,
        cardType = CreditCardType.REVOLVING,
        limit = 3000.0,
        closingDay = 10,
        paymentDay = 5,
    )

    @BeforeEach
    fun setUp() {
        useCase = AddTransactionSaveUseCase()
    }

    @Test
    fun `should return error when amount is zero`() {
        val uiState = AddTransactionUiState(
            amountText = "0",
            description = "Test",
            type = TransactionType.EXPENSE,
            dateStr = todayStr,
        )
        val result = useCase(uiState, null, categories, emptyList(), dateFormat, locale)
        assertTrue(result is AddTransactionSaveResult.Error)
    }

    @Test
    fun `should return error when description is blank`() {
        val uiState = AddTransactionUiState(
            amountText = "100",
            description = "",
            type = TransactionType.EXPENSE,
            dateStr = todayStr,
        )
        val result = useCase(uiState, null, categories, emptyList(), dateFormat, locale)
        assertTrue(result is AddTransactionSaveResult.Error)
    }

    @Test
    fun `should return error when date format is invalid`() {
        val uiState = AddTransactionUiState(
            amountText = "100",
            description = "Test",
            type = TransactionType.EXPENSE,
            dateStr = "invalid-date",
        )
        val result = useCase(uiState, null, categories, emptyList(), dateFormat, locale)
        assertTrue(result is AddTransactionSaveResult.Error)
    }

    @Test
    fun `should return previous month warning when date is previous month`() {
        val lastMonth = today.minusMonths(1)
        val lastMonthStr = lastMonth.format(DateTimeFormatter.ofPattern(dateFormat))
        val uiState = AddTransactionUiState(
            amountText = "100",
            description = "Test",
            type = TransactionType.EXPENSE,
            dateStr = lastMonthStr,
        )
        val result = useCase(uiState, null, categories, emptyList(), dateFormat, locale)
        assertTrue(result is AddTransactionSaveResult.PreviousMonthWarning)
    }

    @Test
    fun `should return ready with single transaction for valid expense`() {
        val uiState = AddTransactionUiState(
            amountText = "100",
            description = "Pizza",
            type = TransactionType.EXPENSE,
            dateStr = todayStr,
            selectedCategory = "food",
        )
        val result = useCase(uiState, null, categories, emptyList(), dateFormat, locale)
        assertTrue(result is AddTransactionSaveResult.Ready)
        val ready = result as AddTransactionSaveResult.Ready
        assertEquals(1, ready.transactions.size)
        val tx = ready.transactions.first()
        assertEquals("Pizza", tx.description)
        assertEquals(100.0, tx.amount)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("food", tx.categoryId)
    }

    @Test
    fun `should return error when installment is used with saldo card`() {
        val uiState = AddTransactionUiState(
            amountText = "1000",
            description = "Installment test",
            type = TransactionType.EXPENSE,
            dateStr = todayStr,
            selectedCategory = "food",
            isCreditCard = true,
            creditCardId = "card_1",
            isInstallment = true,
            installmentsCount = 3,
        )
        val result = useCase(uiState, null, categories, listOf(creditCard), dateFormat, locale)
        assertTrue(result is AddTransactionSaveResult.Error)
    }

    @Test
    fun `should return ready with installment transactions for revolving card`() {
        val uiState = AddTransactionUiState(
            amountText = "600",
            description = "Installment test",
            type = TransactionType.EXPENSE,
            dateStr = todayStr,
            selectedCategory = "food",
            isCreditCard = true,
            creditCardId = "card_2",
            isInstallment = true,
            installmentsCount = 3,
        )
        val result = useCase(uiState, null, categories, listOf(revolvingCard), dateFormat, locale)
        assertTrue(result is AddTransactionSaveResult.Ready)
        val ready = result as AddTransactionSaveResult.Ready
        assertTrue(ready.transactions.size >= 3)
    }

    @Test
    fun `should return ready with credit card mirror transaction for expense`() {
        val uiState = AddTransactionUiState(
            amountText = "200",
            description = "Shopping",
            type = TransactionType.EXPENSE,
            dateStr = todayStr,
            selectedCategory = "food",
            isCreditCard = true,
            creditCardId = "card_1",
        )
        val result = useCase(uiState, null, categories, listOf(creditCard), dateFormat, locale)
        assertTrue(result is AddTransactionSaveResult.Ready)
        val ready = result as AddTransactionSaveResult.Ready
        assertEquals(2, ready.transactions.size)
        val mirrorTx = ready.transactions.first()
        assertTrue(mirrorTx.id.endsWith("_mirror"))
        assertEquals(TransactionType.INCOME, mirrorTx.type)
    }

    @Test
    fun `should return error when credit card flag set but no matching card found`() {
        val uiState = AddTransactionUiState(
            amountText = "200",
            description = "Shopping",
            type = TransactionType.EXPENSE,
            dateStr = todayStr,
            selectedCategory = "food",
            isCreditCard = true,
            creditCardId = "nonexistent",
            recurrenceType = RecurrenceType.NONE,
            ignoreDateWarning = true,
        )
        val result = useCase(uiState, null, categories, emptyList(), dateFormat, locale)
        assertTrue(result is AddTransactionSaveResult.Error)
    }

    @Test
    fun `should return ready with top-up transaction`() {
        val uiState = AddTransactionUiState(
            amountText = "100",
            description = "Top up",
            type = TransactionType.EXPENSE,
            dateStr = todayStr,
            selectedCategory = "food",
            isTopUp = true,
            topUpDestinationId = "pm_1",
        )
        val result = useCase(uiState, null, categories, emptyList(), dateFormat, locale)
        assertTrue(result is AddTransactionSaveResult.Ready)
        val ready = result as AddTransactionSaveResult.Ready
        assertEquals(2, ready.transactions.size)
    }

    @Test
    fun `should return ready with recurrence transactions`() {
        val uiState = AddTransactionUiState(
            amountText = "50",
            description = "Monthly sub",
            type = TransactionType.EXPENSE,
            dateStr = todayStr,
            selectedCategory = "food",
            recurrenceType = RecurrenceType.MONTHLY,
            recurrenceLimit = 3,
            isRecurrenceEnabled = true,
        )
        val result = useCase(uiState, null, categories, emptyList(), dateFormat, locale)
        assertTrue(result is AddTransactionSaveResult.Ready)
        val ready = result as AddTransactionSaveResult.Ready
        assertEquals(3, ready.transactions.size)
    }

    @Test
    fun `should use existing transaction id when editing`() {
        val existingTx = TransactionEntity(
            id = "existing_id",
            date = todayStr,
            description = "Old description",
            amount = 50.0,
            categoryId = "food",
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = todayStr,
            originalAmount = 50.0,
            originalCurrency = "",
        )
        val uiState = AddTransactionUiState(
            amountText = "100",
            description = "Updated desc",
            type = TransactionType.EXPENSE,
            dateStr = todayStr,
            selectedCategory = "food",
        )
        val result = useCase(uiState, existingTx, categories, emptyList(), dateFormat, locale)
        assertTrue(result is AddTransactionSaveResult.Ready)
        val ready = result as AddTransactionSaveResult.Ready
        assertEquals("existing_id", ready.transactions.first().id)
        assertEquals("Updated desc", ready.transactions.first().description)
    }
}
