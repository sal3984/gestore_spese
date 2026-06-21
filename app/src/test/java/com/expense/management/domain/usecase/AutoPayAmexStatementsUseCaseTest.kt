package com.expense.management.domain.usecase

import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexRevolvingStateEntity
import com.expense.management.data.AmexStatementEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("AutoPayAmexStatementsUseCase")
class AutoPayAmexStatementsUseCaseTest {

    private lateinit var useCase: AutoPayAmexStatementsUseCase
    private val today = LocalDate.of(2024, 7, 15)

    @BeforeEach
    fun setUp() {
        useCase = AutoPayAmexStatementsUseCase()
    }

    @Test
    fun `should return result for due non-closed statement with positive payment`() {
        val statement = AmexStatementEntity(
            id = "stmt_1",
            paymentMethodId = "pm_1",
            statementMonth = "2024-06",
            totalExpenses = 500.0,
            paymentMode = "SALDO",
            paymentAmount = 500.0,
            closingDate = "2024-06-30",
            paymentDueDate = "2024-07-10",
            isClosed = false,
        )
        val result = useCase.execute(
            statements = listOf(statement),
            pagoFlexPlans = emptyList(),
            revolvingStates = emptyList(),
            today = today,
        )
        assertEquals(1, result.size)
        assertEquals(500.0, result.first().paymentAmount)
        assertEquals("stmt_1", result.first().statement.id)
    }

    @Test
    fun `should skip closed statements`() {
        val statement = AmexStatementEntity(
            id = "stmt_1",
            paymentMethodId = "pm_1",
            statementMonth = "2024-06",
            totalExpenses = 500.0,
            paymentMode = "SALDO",
            paymentAmount = 500.0,
            closingDate = "2024-06-30",
            paymentDueDate = "2024-07-10",
            isClosed = true,
        )
        val result = useCase.execute(
            statements = listOf(statement),
            pagoFlexPlans = emptyList(),
            revolvingStates = emptyList(),
            today = today,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should skip statement with future due date`() {
        val statement = AmexStatementEntity(
            id = "stmt_1",
            paymentMethodId = "pm_1",
            statementMonth = "2024-07",
            totalExpenses = 300.0,
            paymentMode = "SALDO",
            paymentAmount = 300.0,
            closingDate = "2024-07-31",
            paymentDueDate = "2024-08-20",
            isClosed = false,
        )
        val result = useCase.execute(
            statements = listOf(statement),
            pagoFlexPlans = emptyList(),
            revolvingStates = emptyList(),
            today = today,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should skip statement with zero payment amount`() {
        val statement = AmexStatementEntity(
            id = "stmt_1",
            paymentMethodId = "pm_1",
            statementMonth = "2024-06",
            totalExpenses = 0.0,
            paymentMode = "SALDO",
            paymentAmount = 0.0,
            closingDate = "2024-06-30",
            paymentDueDate = "2024-07-10",
            isClosed = false,
        )
        val result = useCase.execute(
            statements = listOf(statement),
            pagoFlexPlans = emptyList(),
            revolvingStates = emptyList(),
            today = today,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should process multiple statements returning results for each`() {
        val statements = listOf(
            AmexStatementEntity(
                id = "stmt_1",
                paymentMethodId = "pm_1",
                statementMonth = "2024-06",
                totalExpenses = 500.0,
                paymentMode = "SALDO",
                paymentAmount = 500.0,
                closingDate = "2024-06-30",
                paymentDueDate = "2024-07-10",
                isClosed = false,
            ),
            AmexStatementEntity(
                id = "stmt_2",
                paymentMethodId = "pm_2",
                statementMonth = "2024-05",
                totalExpenses = 300.0,
                paymentMode = "SALDO",
                paymentAmount = 300.0,
                closingDate = "2024-05-31",
                paymentDueDate = "2024-06-15",
                isClosed = false,
            ),
        )
        val result = useCase.execute(
            statements = statements,
            pagoFlexPlans = emptyList(),
            revolvingStates = emptyList(),
            today = today,
        )
        assertEquals(2, result.size)
    }

    @Test
    fun `should handle unparseable due date gracefully`() {
        val statement = AmexStatementEntity(
            id = "stmt_1",
            paymentMethodId = "pm_1",
            statementMonth = "2024-06",
            totalExpenses = 500.0,
            paymentMode = "SALDO",
            paymentAmount = 500.0,
            closingDate = "2024-06-30",
            paymentDueDate = "bad-date",
            isClosed = false,
        )
        val result = useCase.execute(
            statements = listOf(statement),
            pagoFlexPlans = emptyList(),
            revolvingStates = emptyList(),
            today = today,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should include pagoflex and revolving data in calculation`() {
        val statement = AmexStatementEntity(
            id = "stmt_1",
            paymentMethodId = "pm_1",
            statementMonth = "2024-06",
            totalExpenses = 1000.0,
            totalPagoflex = 400.0,
            paymentMode = "SALDO",
            paymentAmount = 1000.0,
            closingDate = "2024-06-30",
            paymentDueDate = "2024-07-10",
            isClosed = false,
        )
        val plans = listOf(
            AmexPagoFlexPlanEntity(
                id = "plan_1",
                statementId = "stmt_1",
                transactionId = "tx_1",
                totalAmount = 400.0,
                installmentCount = 2,
                installmentAmount = 200.0,
                paidCount = 1,
                startDate = "2024-06-01",
            ),
        )
        val revolving = AmexRevolvingStateEntity(
            id = "rev_1",
            statementId = "stmt_1",
            carriedForwardDebt = 200.0,
            interestRate = 0.0,
            userPaymentChoice = "SALDO",
        )
        val result = useCase.execute(
            statements = listOf(statement),
            pagoFlexPlans = plans,
            revolvingStates = listOf(revolving),
            today = today,
        )
        assertEquals(1, result.size)
        assertEquals(1000.0, result.first().paymentAmount)
    }
}
