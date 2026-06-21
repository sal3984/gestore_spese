package com.expense.management.domain.usecase

import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity
import com.expense.management.data.AmexStatementEntity
import com.expense.management.data.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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

        assertEquals(1, result.paymentTransactions.size)
        val paymentTx = result.paymentTransactions.first()
        assertEquals("Pagamento Amex 2024-06", paymentTx.description)
        assertEquals(TransactionType.EXPENSE, paymentTx.type)
        assertEquals(1200.0, paymentTx.amount)
        assertNotNull(result.incomeTransaction)
        assertEquals("Rimborso Amex 2024-06", result.incomeTransaction?.description)
        assertEquals(TransactionType.INCOME, result.incomeTransaction?.type)
        assertEquals(1200.0, result.incomeTransaction?.amount)
        assertEquals(0, result.paidInstallments.size)
    }

    @Test
    fun `payment with installment plan pays current installment per plan regardless of due date`() {
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
                dueDate = "2024-07-15",
                amount = 200.0,
                status = "PENDING",
            ),
            AmexPagoFlexScheduledPaymentEntity(
                id = "p2",
                planId = "plan1",
                sequenceNumber = 2,
                dueDate = "2024-08-15",
                amount = 200.0,
                status = "PENDING",
            ),
            AmexPagoFlexScheduledPaymentEntity(
                id = "p3",
                planId = "plan1",
                sequenceNumber = 3,
                dueDate = "2024-09-15",
                amount = 200.0,
                status = "PENDING",
            ),
        )

        val result = useCase.execute(
            statement = statement,
            amount = 1200.0,
            paymentDate = "2024-06-20",
            plans = listOf(plan),
            scheduledPayments = pendingPayments,
        )

        assertNull(result.incomeTransaction)
        assertEquals(1, result.paidInstallments.size)
        assertEquals(listOf("p1"), result.paidInstallments.map { it.id })
        assertEquals(1, result.paymentTransactions.size)
        result.paymentTransactions.forEach { tx ->
            assertEquals(TransactionType.EXPENSE, tx.type)
            assertTrue(!tx.isCreditCard)
            assertEquals("credit_card_payment", tx.categoryId)
        }
        assertEquals("2024-06-20", result.paymentTransactions[0].effectiveDate)
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

        assertEquals(0, result.paidInstallments.size)
        assertEquals(0, result.paymentTransactions.size)
    }
}
