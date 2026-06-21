package com.expense.management.domain.usecase

import com.expense.management.data.KlarnaDetailEntity
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.PaypalDetailEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.PaymentProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.YearMonth

@DisplayName("CalculateBnplProjectionsUseCase")
class CalculateBnplProjectionsUseCaseTest {

    private lateinit var useCase: CalculateBnplProjectionsUseCase
    private val targetMonth = YearMonth.of(2024, 6)
    private val paypalMethod = PaymentMethodEntity(
        id = "paypal_1",
        name = "PayPal Account",
        provider = PaymentProvider.PAYPAL,
    )
    private val klarnaMethod = PaymentMethodEntity(
        id = "klarna_1",
        name = "Klarna Account",
        provider = PaymentProvider.KLARNA,
    )
    private val cashMethod = PaymentMethodEntity(
        id = "cash_1",
        name = "Cash",
        provider = PaymentProvider.CASH,
    )

    @BeforeEach
    fun setUp() {
        useCase = CalculateBnplProjectionsUseCase()
    }

    @Test
    fun `should return empty when no BNPL payment methods`() {
        val result = useCase.execute(
            allTransactions = emptyList(),
            allPaymentMethods = listOf(cashMethod),
            paypalDetails = emptyList(),
            klarnaDetails = emptyList(),
            targetMonth = targetMonth,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should return empty when no BNPL transactions`() {
        val result = useCase.execute(
            allTransactions = emptyList(),
            allPaymentMethods = listOf(paypalMethod),
            paypalDetails = listOf(
                PaypalDetailEntity(paymentMethodId = "paypal_1", email = "test@test.com"),
            ),
            klarnaDetails = emptyList(),
            targetMonth = targetMonth,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should project PayPal installments for transactions in target month`() {
        val transaction = TransactionEntity(
            id = "tx_1",
            date = "2024-06-01",
            description = "PayPal purchase",
            amount = 300.0,
            categoryId = "shopping",
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = "2024-06-01",
            originalAmount = 300.0,
            originalCurrency = "",
            paymentMethodId = "paypal_1",
        )
        val result = useCase.execute(
            allTransactions = listOf(transaction),
            allPaymentMethods = listOf(paypalMethod),
            paypalDetails = listOf(
                PaypalDetailEntity(paymentMethodId = "paypal_1", email = "test@test.com", bnplInstallmentCount = 3, bnplCycleDays = 14),
            ),
            klarnaDetails = emptyList(),
            targetMonth = targetMonth,
        )
        assertEquals(1, result.size)
        val projection = result.first()
        assertEquals("PayPal Account", projection.methodName)
        assertEquals(PaymentProvider.PAYPAL, projection.provider)
        assertEquals(3, projection.installments.size)
        assertEquals(100.0, projection.installments[0].amount, 0.001)
        assertTrue(projection.installments.all { it.transactionId == "tx_1" })
    }

    @Test
    fun `should project Klarna installments for transactions in target month`() {
        val transaction = TransactionEntity(
            id = "tx_1",
            date = "2024-06-01",
            description = "Klarna purchase",
            amount = 400.0,
            categoryId = "shopping",
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = "2024-06-01",
            originalAmount = 400.0,
            originalCurrency = "",
            paymentMethodId = "klarna_1",
        )
        val result = useCase.execute(
            allTransactions = listOf(transaction),
            allPaymentMethods = listOf(klarnaMethod),
            paypalDetails = emptyList(),
            klarnaDetails = listOf(
                KlarnaDetailEntity(paymentMethodId = "klarna_1", bnplInstallmentCount = 4, bnplCycleDays = 14),
            ),
            targetMonth = targetMonth,
        )
        assertEquals(1, result.size)
        val projection = result.first()
        assertEquals("Klarna Account", projection.methodName)
        assertEquals(PaymentProvider.KLARNA, projection.provider)
        assertEquals(3, projection.installments.size)
        assertEquals(100.0, projection.installments[0].amount, 0.001)
    }

    @Test
    fun `should exclude transactions outside target month`() {
        val transaction = TransactionEntity(
            id = "tx_1",
            date = "2024-05-01",
            description = "Old purchase",
            amount = 300.0,
            categoryId = "shopping",
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = "2024-05-01",
            originalAmount = 300.0,
            originalCurrency = "",
            paymentMethodId = "paypal_1",
        )
        val result = useCase.execute(
            allTransactions = listOf(transaction),
            allPaymentMethods = listOf(paypalMethod),
            paypalDetails = listOf(
                PaypalDetailEntity(paymentMethodId = "paypal_1", email = "test@test.com", bnplInstallmentCount = 3, bnplCycleDays = 14),
            ),
            klarnaDetails = emptyList(),
            targetMonth = targetMonth,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should handle multiple BNPL methods with transactions`() {
        val paypalTx = TransactionEntity(
            id = "tx_1",
            date = "2024-06-01",
            description = "PayPal buy",
            amount = 150.0,
            categoryId = "shopping",
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = "2024-06-01",
            originalAmount = 150.0,
            originalCurrency = "",
            paymentMethodId = "paypal_1",
        )
        val klarnaTx = TransactionEntity(
            id = "tx_2",
            date = "2024-06-15",
            description = "Klarna buy",
            amount = 200.0,
            categoryId = "electronics",
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = "2024-06-15",
            originalAmount = 200.0,
            originalCurrency = "",
            paymentMethodId = "klarna_1",
        )
        val result = useCase.execute(
            allTransactions = listOf(paypalTx, klarnaTx),
            allPaymentMethods = listOf(paypalMethod, klarnaMethod),
            paypalDetails = listOf(
                PaypalDetailEntity(paymentMethodId = "paypal_1", email = "test@test.com", bnplInstallmentCount = 3, bnplCycleDays = 14),
            ),
            klarnaDetails = listOf(
                KlarnaDetailEntity(paymentMethodId = "klarna_1", bnplInstallmentCount = 4, bnplCycleDays = 14),
            ),
            targetMonth = targetMonth,
        )
        assertEquals(2, result.size)
        val paypalProjection = result.find { it.provider == PaymentProvider.PAYPAL }
        val klarnaProjection = result.find { it.provider == PaymentProvider.KLARNA }
        assertTrue(paypalProjection != null)
        assertTrue(klarnaProjection != null)
    }

    @Test
    fun `should handle transaction with unparseable date`() {
        val transaction = TransactionEntity(
            id = "tx_1",
            date = "bad-date",
            description = "Bad date tx",
            amount = 300.0,
            categoryId = "shopping",
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = "bad-date",
            originalAmount = 300.0,
            originalCurrency = "",
            paymentMethodId = "paypal_1",
        )
        val result = useCase.execute(
            allTransactions = listOf(transaction),
            allPaymentMethods = listOf(paypalMethod),
            paypalDetails = listOf(
                PaypalDetailEntity(paymentMethodId = "paypal_1", email = "test@test.com", bnplInstallmentCount = 3, bnplCycleDays = 14),
            ),
            klarnaDetails = emptyList(),
            targetMonth = targetMonth,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should skip method when detail not found`() {
        val transaction = TransactionEntity(
            id = "tx_1",
            date = "2024-06-01",
            description = "Purchase",
            amount = 100.0,
            categoryId = "shopping",
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = "2024-06-01",
            originalAmount = 100.0,
            originalCurrency = "",
            paymentMethodId = "paypal_1",
        )
        val result = useCase.execute(
            allTransactions = listOf(transaction),
            allPaymentMethods = listOf(paypalMethod),
            paypalDetails = emptyList(),
            klarnaDetails = emptyList(),
            targetMonth = targetMonth,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should calculate totalExpected from installments`() {
        val transaction = TransactionEntity(
            id = "tx_1",
            date = "2024-06-01",
            description = "Test",
            amount = 300.0,
            categoryId = "shopping",
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = "2024-06-01",
            originalAmount = 300.0,
            originalCurrency = "",
            paymentMethodId = "paypal_1",
        )
        val result = useCase.execute(
            allTransactions = listOf(transaction),
            allPaymentMethods = listOf(paypalMethod),
            paypalDetails = listOf(
                PaypalDetailEntity(paymentMethodId = "paypal_1", email = "test@test.com", bnplInstallmentCount = 3, bnplCycleDays = 14),
            ),
            klarnaDetails = emptyList(),
            targetMonth = targetMonth,
        )
        assertEquals(1, result.size)
        assertEquals(300.0, result.first().totalExpected, 0.001)
    }
}
