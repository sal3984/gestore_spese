package com.expense.management.domain.usecase

import com.expense.management.data.CategoryEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.YearMonth

@DisplayName("CalculateReportUseCase")
class CalculateReportUseCaseTest {

    private lateinit var useCase: CalculateReportUseCase
    private val foodCategory = CategoryEntity(id = "food", label = "Food", icon = "🍕", type = TransactionType.EXPENSE)
    private val transportCategory = CategoryEntity(id = "transport", label = "Transport", icon = "🚗", type = TransactionType.EXPENSE)
    private val salaryCategory = CategoryEntity(id = "salary", label = "Salary", icon = "💰", type = TransactionType.INCOME)
    private val otherCategory = CategoryEntity(id = "other", label = "Other", icon = "📦", type = TransactionType.EXPENSE)
    private val categories = listOf(foodCategory, transportCategory, salaryCategory, otherCategory)

    @BeforeEach
    fun setUp() {
        useCase = CalculateReportUseCase()
    }

    @Test
    fun `should return empty report when no transactions`() {
        val start = YearMonth.of(2024, 1)
        val end = YearMonth.of(2024, 3)
        val result = useCase.execute(emptyList(), categories, start, end)
        assertEquals(0.0, result.totalIncome)
        assertEquals(0.0, result.totalExpense)
        assertEquals(0.0, result.savings)
        assertEquals(0.0, result.totalMonthlyExpense)
        assertEquals(3, result.monthlyBalances.size)
        assertEquals(0.0, result.monthlyBalances.first().balance)
    }

    @Test
    fun `should calculate totals for single expense`() {
        val transactions = listOf(
            TransactionEntity(
                id = "tx_1",
                date = "2024-01-15",
                description = "Pizza",
                amount = 50.0,
                categoryId = "food",
                type = TransactionType.EXPENSE,
                isCreditCard = false,
                effectiveDate = "2024-01-15",
                originalAmount = 50.0,
                originalCurrency = "",
            ),
        )
        val start = YearMonth.of(2024, 1)
        val end = YearMonth.of(2024, 1)
        val result = useCase.execute(transactions, categories, start, end)
        assertEquals(0.0, result.totalIncome)
        assertEquals(50.0, result.totalExpense)
        assertEquals(-50.0, result.savings)
        assertEquals(1, result.expenseByCategory.size)
        assertEquals("Food", result.expenseByCategory.first().categoryName)
        assertEquals(50.0, result.expenseByCategory.first().amount)
    }

    @Test
    fun `should calculate income and expense`() {
        val transactions = listOf(
            TransactionEntity(
                id = "tx_1",
                date = "2024-01-10",
                description = "Salary",
                amount = 2000.0,
                categoryId = "salary",
                type = TransactionType.INCOME,
                isCreditCard = false,
                effectiveDate = "2024-01-10",
                originalAmount = 2000.0,
                originalCurrency = "",
            ),
            TransactionEntity(
                id = "tx_2",
                date = "2024-01-15",
                description = "Rent",
                amount = 800.0,
                categoryId = "other",
                type = TransactionType.EXPENSE,
                isCreditCard = false,
                effectiveDate = "2024-01-15",
                originalAmount = 800.0,
                originalCurrency = "",
            ),
        )
        val start = YearMonth.of(2024, 1)
        val end = YearMonth.of(2024, 1)
        val result = useCase.execute(transactions, categories, start, end)
        assertEquals(2000.0, result.totalIncome)
        assertEquals(800.0, result.totalExpense)
        assertEquals(1200.0, result.savings)
        assertEquals(2000.0 - 800.0, result.monthlyBalances.first().balance)
    }

    @Test
    fun `should calculate monthly balances across multiple months`() {
        val transactions = listOf(
            TransactionEntity(
                id = "tx_1",
                date = "2024-01-15",
                description = "Salary Jan",
                amount = 2000.0,
                categoryId = "salary",
                type = TransactionType.INCOME,
                isCreditCard = false,
                effectiveDate = "2024-01-15",
                originalAmount = 2000.0,
                originalCurrency = "",
            ),
            TransactionEntity(
                id = "tx_2",
                date = "2024-02-15",
                description = "Salary Feb",
                amount = 2000.0,
                categoryId = "salary",
                type = TransactionType.INCOME,
                isCreditCard = false,
                effectiveDate = "2024-02-15",
                originalAmount = 2000.0,
                originalCurrency = "",
            ),
            TransactionEntity(
                id = "tx_3",
                date = "2024-02-20",
                description = "Groceries",
                amount = 300.0,
                categoryId = "food",
                type = TransactionType.EXPENSE,
                isCreditCard = false,
                effectiveDate = "2024-02-20",
                originalAmount = 300.0,
                originalCurrency = "",
            ),
        )
        val start = YearMonth.of(2024, 1)
        val end = YearMonth.of(2024, 2)
        val result = useCase.execute(transactions, categories, start, end)
        assertEquals(4000.0, result.totalIncome)
        assertEquals(300.0, result.totalExpense)
        assertEquals(2, result.monthlyBalances.size)
        assertEquals(2000.0, result.monthlyBalances[0].balance)
        assertEquals(1700.0, result.monthlyBalances[1].balance)
    }

    @Test
    fun `should group expenses by category`() {
        val transactions = listOf(
            TransactionEntity(
                id = "tx_1",
                date = "2024-01-10",
                description = "Pizza",
                amount = 50.0,
                categoryId = "food",
                type = TransactionType.EXPENSE,
                isCreditCard = false,
                effectiveDate = "2024-01-10",
                originalAmount = 50.0,
                originalCurrency = "",
            ),
            TransactionEntity(
                id = "tx_2",
                date = "2024-01-11",
                description = "Pasta",
                amount = 30.0,
                categoryId = "food",
                type = TransactionType.EXPENSE,
                isCreditCard = false,
                effectiveDate = "2024-01-11",
                originalAmount = 30.0,
                originalCurrency = "",
            ),
            TransactionEntity(
                id = "tx_3",
                date = "2024-01-12",
                description = "Bus ticket",
                amount = 20.0,
                categoryId = "transport",
                type = TransactionType.EXPENSE,
                isCreditCard = false,
                effectiveDate = "2024-01-12",
                originalAmount = 20.0,
                originalCurrency = "",
            ),
        )
        val start = YearMonth.of(2024, 1)
        val end = YearMonth.of(2024, 1)
        val result = useCase.execute(transactions, categories, start, end)
        assertEquals(2, result.expenseByCategory.size)
        val foodSummary = result.expenseByCategory.find { it.categoryId == "food" }
        val transportSummary = result.expenseByCategory.find { it.categoryId == "transport" }
        assertEquals(80.0, foodSummary!!.amount)
        assertEquals(20.0, transportSummary!!.amount)
    }

    @Test
    fun `should skip transactions with unparseable dates`() {
        val transactions = listOf(
            TransactionEntity(
                id = "tx_1",
                date = "invalid-date",
                description = "Bad",
                amount = 100.0,
                categoryId = "food",
                type = TransactionType.EXPENSE,
                isCreditCard = false,
                effectiveDate = "invalid-date",
                originalAmount = 100.0,
                originalCurrency = "",
            ),
        )
        val start = YearMonth.of(2024, 1)
        val end = YearMonth.of(2024, 1)
        val result = useCase.execute(transactions, categories, start, end)
        assertEquals(0.0, result.totalExpense)
        assertEquals(0.0, result.savings)
    }

    @Test
    fun `should use other category when category not found`() {
        val transactions = listOf(
            TransactionEntity(
                id = "tx_1",
                date = "2024-01-15",
                description = "Unknown cat",
                amount = 75.0,
                categoryId = "nonexistent",
                type = TransactionType.EXPENSE,
                isCreditCard = false,
                effectiveDate = "2024-01-15",
                originalAmount = 75.0,
                originalCurrency = "",
            ),
        )
        val start = YearMonth.of(2024, 1)
        val end = YearMonth.of(2024, 1)
        val result = useCase.execute(transactions, categories, start, end)
        assertEquals(75.0, result.totalExpense)
        val summary = result.expenseByCategory.first()
        assertEquals("Other", summary.categoryName)
    }
}
