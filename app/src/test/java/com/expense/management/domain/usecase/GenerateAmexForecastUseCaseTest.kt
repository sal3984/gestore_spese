package com.expense.management.domain.usecase

import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexRevolvingStateEntity
import com.expense.management.domain.model.AmexPaymentMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("GenerateAmexForecastUseCase")
class GenerateAmexForecastUseCaseTest {

    private lateinit var useCase: GenerateAmexForecastUseCase
    private val statementMonth = "2024-06"

    @BeforeEach
    fun setUp() {
        useCase = GenerateAmexForecastUseCase()
    }

    @Test
    fun `should forecast with SALDO mode paying full carried debt in first month`() {
        val revolving = AmexRevolvingStateEntity(
            id = "rev_1",
            statementId = "stmt_1",
            carriedForwardDebt = 1200.0,
            interestRate = 0.0,
            userPaymentChoice = "SALDO",
        )
        val result = useCase.execute(
            statementMonth = statementMonth,
            pagoFlexPlans = emptyList(),
            revolvingState = revolving,
            paymentMode = AmexPaymentMode.SALDO,
            paymentAmount = 0.0,
            interestRate = 0.0,
            monthsToForecast = 3,
        )
        assertEquals(3, result.size)
        assertEquals(1200.0, result[0].revolvingPayment)
        assertEquals(1200.0, result[0].totalDue)
        assertTrue(result[0].carriedForwardAfter < 0.01)
        assertEquals(0.0, result[1].revolvingPayment)
        assertEquals(0.0, result[1].totalDue)
    }

    @Test
    fun `should forecast with MINIMUM mode paying 5 percent`() {
        val revolving = AmexRevolvingStateEntity(
            id = "rev_1",
            statementId = "stmt_1",
            carriedForwardDebt = 1000.0,
            interestRate = 0.0,
            userPaymentChoice = "MINIMUM",
        )
        val result = useCase.execute(
            statementMonth = statementMonth,
            pagoFlexPlans = emptyList(),
            revolvingState = revolving,
            paymentMode = AmexPaymentMode.MINIMUM,
            paymentAmount = 0.0,
            interestRate = 0.0,
            monthsToForecast = 2,
        )
        assertEquals(2, result.size)
        assertEquals(50.0, result[0].revolvingPayment)
        assertEquals(950.0, result[0].carriedForwardAfter)
    }

    @Test
    fun `should forecast with FIXED mode`() {
        val revolving = AmexRevolvingStateEntity(
            id = "rev_1",
            statementId = "stmt_1",
            carriedForwardDebt = 1000.0,
            interestRate = 0.0,
            userPaymentChoice = "FIXED",
        )
        val result = useCase.execute(
            statementMonth = statementMonth,
            pagoFlexPlans = emptyList(),
            revolvingState = revolving,
            paymentMode = AmexPaymentMode.FIXED,
            paymentAmount = 300.0,
            interestRate = 0.0,
            monthsToForecast = 4,
        )
        assertEquals(300.0, result[0].revolvingPayment)
        assertEquals(700.0, result[0].carriedForwardAfter)
    }

    @Test
    fun `should forecast with PAGOFLEX_ONLY mode no revolving payment`() {
        val revolving = AmexRevolvingStateEntity(
            id = "rev_1",
            statementId = "stmt_1",
            carriedForwardDebt = 1000.0,
            interestRate = 0.0,
            userPaymentChoice = "PAGOFLEX_ONLY",
        )
        val result = useCase.execute(
            statementMonth = statementMonth,
            pagoFlexPlans = emptyList(),
            revolvingState = revolving,
            paymentMode = AmexPaymentMode.PAGOFLEX_ONLY,
            paymentAmount = 0.0,
            interestRate = 0.0,
            monthsToForecast = 1,
        )
        assertEquals(0.0, result[0].revolvingPayment)
        assertEquals(1000.0, result[0].carriedForwardAfter)
    }

    @Test
    fun `should include pagoflex quotas in forecast`() {
        val plans = listOf(
            AmexPagoFlexPlanEntity(
                id = "plan_1",
                statementId = "stmt_1",
                transactionId = "tx_1",
                totalAmount = 600.0,
                installmentCount = 3,
                installmentAmount = 200.0,
                paidCount = 0,
                startDate = "2024-06-01",
            ),
        )
        val result = useCase.execute(
            statementMonth = statementMonth,
            pagoFlexPlans = plans,
            revolvingState = null,
            paymentMode = AmexPaymentMode.PAGOFLEX_ONLY,
            paymentAmount = 0.0,
            interestRate = 0.0,
            monthsToForecast = 4,
        )
        assertEquals(200.0, result[0].pagoflexQuota)
        assertEquals(200.0, result[1].pagoflexQuota)
        assertEquals(200.0, result[2].pagoflexQuota)
        assertEquals(0.0, result[3].pagoflexQuota)
    }

    @Test
    fun `should calculate interest on carried forward debt`() {
        val revolving = AmexRevolvingStateEntity(
            id = "rev_1",
            statementId = "stmt_1",
            carriedForwardDebt = 1000.0,
            interestRate = 0.0,
            userPaymentChoice = "SALDO",
        )
        val result = useCase.execute(
            statementMonth = statementMonth,
            pagoFlexPlans = emptyList(),
            revolvingState = revolving,
            paymentMode = AmexPaymentMode.SALDO,
            paymentAmount = 0.0,
            interestRate = 12.0,
            monthsToForecast = 1,
        )
        val monthlyRate = 12.0 / 100.0 / 12.0
        val expectedInterest = 1000.0 * monthlyRate
        assertEquals(expectedInterest, result[0].interest, 0.001)
    }

    @Test
    fun `should return forecast months with correct labels`() {
        val result = useCase.execute(
            statementMonth = "2024-01",
            pagoFlexPlans = emptyList(),
            revolvingState = null,
            paymentMode = AmexPaymentMode.SALDO,
            paymentAmount = 0.0,
            interestRate = 0.0,
            monthsToForecast = 2,
        )
        assertEquals(2, result.size)
        assertEquals(1, result[0].monthOffset)
        assertEquals(2, result[1].monthOffset)
    }

    @Test
    fun `should handle invalid statement month by using current month`() {
        val result = useCase.execute(
            statementMonth = "not-a-month",
            pagoFlexPlans = emptyList(),
            revolvingState = null,
            paymentMode = AmexPaymentMode.SALDO,
            paymentAmount = 0.0,
            interestRate = 0.0,
            monthsToForecast = 1,
        )
        assertEquals(1, result.size)
        assertEquals(1, result[0].monthOffset)
    }

    @Test
    fun `should handle no revolving state gracefully`() {
        val result = useCase.execute(
            statementMonth = statementMonth,
            pagoFlexPlans = emptyList(),
            revolvingState = null,
            paymentMode = AmexPaymentMode.SALDO,
            paymentAmount = 0.0,
            interestRate = 0.0,
            monthsToForecast = 1,
        )
        assertEquals(0.0, result[0].carriedForwardAfter)
        assertEquals(0.0, result[0].totalDue)
    }
}
