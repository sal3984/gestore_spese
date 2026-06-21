package com.expense.management.domain.usecase

import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity
import com.expense.management.data.AmexStatementEntity
import com.expense.management.data.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PayAmexStatementUseCaseTest {

    private val statement = AmexStatementEntity(
        id = "stmt1",
        paymentMethodId = "amex1",
        statementMonth = "2024-06",
        totalExpenses = 1200.0,
        totalPagoflex = 1200.0,
        revolvingBalance = 0.0,
        paymentMode = "SALDO",
        paymentAmount = 1200.0,
        closingDate = "2024-06-30",
        paymentDueDate = "2024-07-15",
    )

    @Test
    fun `payment without installment plan creates expense and income transactions`() {
        val useCase = PayAmexStatementUseCase()

        val result = useCase.execute(
            statement = statement,
            amount = 1200.0,
            paymentDate = "2024-07-15",
            plans = emptyList(),
            scheduledPayments = emptyList(),
        )

        assertNotNull(result.paymentTransaction)
        assertEquals("Pagamento Amex 2024-06", result.paymentTransaction?.description)
        assertEquals(TransactionType.EXPENSE, result.paymentTransaction?.type)
        assertEquals(1200.0, result.paymentTransaction?.amount)
        assertNotNull(result.incomeTransaction)
        assertEquals("Rimborso Amex 2024-06", result.incomeTransaction?.description)
        assertEquals(TransactionType.INCOME, result.incomeTransaction?.type)
        assertEquals(1200.0, result.incomeTransaction?.amount)
        assertEquals(0, result.paymentsToMarkPaid.size)
    }

    @Test
    fun `payment with installment plan creates no cash flow transaction and marks pending payments paid`() {
        val useCase = PayAmexStatementUseCase()
        val plan = AmexPagoFlexPlanEntity(
            id = "plan1",
            statementId = "stmt1",
            transactionId = "tx1",
            totalAmount = 1200.0,
            installmentCount = 6,
            installmentAmount = 200.0,
            paidCount = 0,
            startDate = "2024-06-15",
        )
        val pendingPayments = listOf(
            AmexPagoFlexScheduledPaymentEntity(
                id = "p1",
                planId = "plan1",
                sequenceNumber = 1,
                dueDate = "2024-06-15",
                amount = 200.0,
                status = "PENDING",
            ),
            AmexPagoFlexScheduledPaymentEntity(
                id = "p2",
                planId = "plan1",
                sequenceNumber = 2,
                dueDate = "2024-07-15",
                amount = 200.0,
                status = "PENDING",
            ),
        )
        val paidPayment = AmexPagoFlexScheduledPaymentEntity(
            id = "p3",
            planId = "plan1",
            sequenceNumber = 0,
            dueDate = "2024-05-15",
            amount = 200.0,
            status = "PAID",
        )

        val result = useCase.execute(
            statement = statement,
            amount = 1200.0,
            paymentDate = "2024-07-15",
            plans = listOf(plan),
            scheduledPayments = pendingPayments + paidPayment,
        )

        assertNull(result.paymentTransaction)
        assertNull(result.incomeTransaction)
        assertEquals(2, result.paymentsToMarkPaid.size)
        assertEquals(listOf("p1", "p2"), result.paymentsToMarkPaid.map { it.id })
    }

    @Test
    fun `payment with installment plan ignores payments from other plans`() {
        val useCase = PayAmexStatementUseCase()
        val plan = AmexPagoFlexPlanEntity(
            id = "plan1",
            statementId = "stmt1",
            transactionId = "tx1",
            totalAmount = 1200.0,
            installmentCount = 6,
            installmentAmount = 200.0,
            paidCount = 0,
            startDate = "2024-06-15",
        )
        val otherPlanPayment = AmexPagoFlexScheduledPaymentEntity(
            id = "pOther",
            planId = "plan2",
            sequenceNumber = 1,
            dueDate = "2024-06-15",
            amount = 200.0,
            status = "PENDING",
        )

        val result = useCase.execute(
            statement = statement,
            amount = 1200.0,
            paymentDate = "2024-07-15",
            plans = listOf(plan),
            scheduledPayments = listOf(otherPlanPayment),
        )

        assertEquals(0, result.paymentsToMarkPaid.size)
    }
}
