package com.expense.management.domain.usecase

import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity
import com.expense.management.data.AmexStatementEntity
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.TransactionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("MaterializeDueAmexInstallmentsUseCase")
class MaterializeDueAmexInstallmentsUseCaseTest {

    private lateinit var repository: ExpenseRepository
    private lateinit var useCase: MaterializeDueAmexInstallmentsUseCase

    private val plan = AmexPagoFlexPlanEntity(
        id = "plan1",
        statementId = "stmt1",
        transactionId = "tx1",
        totalAmount = 600.0,
        installmentCount = 6,
        installmentAmount = 100.0,
        paidCount = 0,
        startDate = "2026-05-15",
    )

    private val statement = AmexStatementEntity(
        id = "stmt1",
        paymentMethodId = "amex1",
        statementMonth = "2026-05",
        paymentMode = "SALDO",
        closingDate = "2026-05-30",
        paymentDueDate = "2026-06-15",
    )

    private fun pendingPayment(id: String, sequenceNumber: Int, dueDate: String) =
        AmexPagoFlexScheduledPaymentEntity(
            id = id,
            planId = "plan1",
            sequenceNumber = sequenceNumber,
            dueDate = dueDate,
            amount = 100.0,
            status = "PENDING",
        )

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = MaterializeDueAmexInstallmentsUseCase(repository)
        coEvery { repository.withTransaction<Unit>(any()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        coEvery { repository.getAllAmexPagoFlexPlans() } returns listOf(plan)
        coEvery { repository.getAmexStatementById("stmt1") } returns statement
        coEvery { repository.getAllCreditCardDetails() } returns emptyList()
        coEvery { repository.getAmexScheduledPaymentsForPlan("plan1") } returns emptyList()
    }

    @Test
    fun `materializes every overdue installment, not only the current month`() {
        coEvery { repository.getPendingAmexScheduledPaymentsDueBy(any()) } returns listOf(
            pendingPayment("p1", 1, "2026-08-15"),
            pendingPayment("p2", 2, "2026-09-15"),
            pendingPayment("p3", 3, "2026-10-15"),
        )

        val materialized = runBlocking { useCase(LocalDate.parse("2026-10-20")) }

        assertEquals(3, materialized)
        coVerify(exactly = 3) { repository.insertTransaction(any()) }
        coVerify(exactly = 1) { repository.markAmexScheduledPaymentAsPaid("p1", any()) }
        coVerify(exactly = 1) { repository.markAmexScheduledPaymentAsPaid("p2", any()) }
        coVerify(exactly = 1) { repository.markAmexScheduledPaymentAsPaid("p3", any()) }
    }

    @Test
    fun `asks the database only for installments due by today`() {
        coEvery { repository.getPendingAmexScheduledPaymentsDueBy(any()) } returns emptyList()

        val materialized = runBlocking { useCase(LocalDate.parse("2026-10-20")) }

        assertEquals(0, materialized)
        coVerify(exactly = 1) { repository.getPendingAmexScheduledPaymentsDueBy("2026-10-20") }
        coVerify(exactly = 0) { repository.insertTransaction(any()) }
    }

    @Test
    fun `writes the transaction and the paid flag in one database transaction`() {
        coEvery { repository.getPendingAmexScheduledPaymentsDueBy(any()) } returns listOf(
            pendingPayment("p1", 1, "2026-09-15"),
        )

        runBlocking { useCase(LocalDate.parse("2026-10-20")) }

        coVerify(exactly = 1) { repository.withTransaction<Unit>(any()) }
    }

    @Test
    fun `installment leaves the current account on its own due date`() {
        coEvery { repository.getPendingAmexScheduledPaymentsDueBy(any()) } returns listOf(
            pendingPayment("p1", 2, "2026-09-15"),
        )
        val captured = slot<TransactionEntity>()
        coEvery { repository.insertTransaction(capture(captured)) } returns Unit

        runBlocking { useCase(LocalDate.parse("2026-10-20")) }

        assertEquals("2026-09-15", captured.captured.effectiveDate)
        assertEquals(100.0, captured.captured.amount, 0.01)
        assertFalse(captured.captured.isCreditCard)
        assertEquals("plan1", captured.captured.groupId)
        assertEquals(2, captured.captured.installmentNumber)
    }

    @Test
    fun `refreshes the plan paid count once per touched plan`() {
        coEvery { repository.getPendingAmexScheduledPaymentsDueBy(any()) } returns listOf(
            pendingPayment("p1", 1, "2026-08-15"),
            pendingPayment("p2", 2, "2026-09-15"),
        )

        runBlocking { useCase(LocalDate.parse("2026-10-20")) }

        coVerify(exactly = 1) { repository.updateAmexPagoFlexPaidCount("plan1", any()) }
    }
}
