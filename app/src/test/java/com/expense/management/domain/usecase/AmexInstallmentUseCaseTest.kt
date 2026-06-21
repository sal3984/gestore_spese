package com.expense.management.domain.usecase

import com.expense.management.domain.model.AmexInstallmentStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.round

class AmexInstallmentUseCaseTest {

    private fun Double.round2(): Double = round(this * 100.0) / 100.0

    @Test
    fun `build fixed duration schedule spreads amount over months`() {
        val useCase = BuildAmexScheduledPaymentsUseCase()

        val payments = useCase.execute(
            planId = "plan1",
            totalAmount = 1200.0,
            strategy = AmexInstallmentStrategy.FixedDuration(6),
            startDate = "2024-06-15",
        )

        assertEquals(6, payments.size)
        assertEquals("2024-06-15", payments[0].dueDate)
        assertEquals("2024-11-15", payments[5].dueDate)
        assertTrue(payments.take(5).all { it.amount == 200.0 })
        assertEquals(200.0, payments[5].amount, 0.01)
        assertEquals(1200.0, payments.sumOf { it.amount }, 0.01)
    }

    @Test
    fun `build fixed amount schedule creates correct count and residual`() {
        val useCase = BuildAmexScheduledPaymentsUseCase()

        val payments = useCase.execute(
            planId = "plan1",
            totalAmount = 1200.0,
            strategy = AmexInstallmentStrategy.FixedAmount(250.0),
            startDate = "2024-06-15",
        )

        assertEquals(5, payments.size)
        assertTrue(payments.take(4).all { it.amount == 250.0 })
        assertEquals(200.0, payments[4].amount, 0.01)
        assertEquals(1200.0, payments.sumOf { it.amount }, 0.01)
    }

    @Test
    fun `build schedule throws when amount is not positive`() {
        val useCase = BuildAmexScheduledPaymentsUseCase()

        assertThrows(IllegalArgumentException::class.java) {
            useCase.execute(
                planId = "plan1",
                totalAmount = 1200.0,
                strategy = AmexInstallmentStrategy.FixedAmount(0.0),
                startDate = "2024-06-15",
            )
        }
    }

    @Test
    fun `build schedule throws when duration is not positive`() {
        val useCase = BuildAmexScheduledPaymentsUseCase()

        assertThrows(IllegalArgumentException::class.java) {
            useCase.execute(
                planId = "plan1",
                totalAmount = 1200.0,
                strategy = AmexInstallmentStrategy.FixedDuration(0),
                startDate = "2024-06-15",
            )
        }
    }

    @Test
    fun `create plan with fixed duration sets plan metadata`() {
        val useCase = CreateAmexInstallmentPlanUseCase()

        val (plan, payments) = useCase.execute(
            planId = "plan1",
            statementId = "stmt1",
            transactionId = "tx1",
            totalAmount = 1200.0,
            startDate = "2024-06-15",
            strategy = AmexInstallmentStrategy.FixedDuration(6),
        )

        assertEquals("plan1", plan.id)
        assertEquals(6, plan.installmentCount)
        assertEquals("FIXED_DURATION", plan.planType)
        assertNull(plan.initialInstallmentAmount)
        assertEquals(1200.0, payments.sumOf { it.amount }, 0.01)
    }

    @Test
    fun `create plan with fixed amount sets plan metadata`() {
        val useCase = CreateAmexInstallmentPlanUseCase()

        val (plan, payments) = useCase.execute(
            planId = "plan1",
            statementId = "stmt1",
            transactionId = "tx1",
            totalAmount = 1200.0,
            startDate = "2024-06-15",
            strategy = AmexInstallmentStrategy.FixedAmount(250.0),
        )

        assertEquals("FIXED_AMOUNT", plan.planType)
        assertEquals(250.0, plan.initialInstallmentAmount)
        assertEquals(5, plan.installmentCount)
        assertEquals(1200.0, payments.sumOf { it.amount }, 0.01)
    }

    @Test
    fun `recalculate before due date changes current month installment`() {
        val buildUseCase = BuildAmexScheduledPaymentsUseCase()
        val payments = buildUseCase.execute(
            planId = "plan1",
            totalAmount = 1200.0,
            strategy = AmexInstallmentStrategy.FixedDuration(6),
            startDate = "2024-06-15",
        )
        val plan = com.expense.management.data.AmexPagoFlexPlanEntity(
            id = "plan1",
            statementId = "stmt1",
            transactionId = "tx1",
            totalAmount = 1200.0,
            installmentCount = 6,
            installmentAmount = 200.0,
            paidCount = 0,
            startDate = "2024-06-15",
            planType = "FIXED_DURATION",
            initialInstallmentAmount = null,
        )

        val useCase = RecalculateAmexInstallmentPlanUseCase()
        val (updatedPlan, newPayments) = useCase.execute(
            existingPlan = plan,
            existingPayments = payments,
            newStrategy = AmexInstallmentStrategy.FixedAmount(150.0),
            today = "2024-06-10",
            currentStatementPaymentDueDate = "2024-06-20",
        )

        assertTrue(newPayments.any { it.dueDate == "2024-06-15" })
        assertEquals(150.0, newPayments.first { it.dueDate == "2024-06-15" }.amount, 0.01)
        assertTrue(updatedPlan.installmentCount > 6)
    }

    @Test
    fun `recalculate after due date keeps current month frozen`() {
        val buildUseCase = BuildAmexScheduledPaymentsUseCase()
        val payments = buildUseCase.execute(
            planId = "plan1",
            totalAmount = 1200.0,
            strategy = AmexInstallmentStrategy.FixedDuration(6),
            startDate = "2024-06-15",
        )
        val plan = com.expense.management.data.AmexPagoFlexPlanEntity(
            id = "plan1",
            statementId = "stmt1",
            transactionId = "tx1",
            totalAmount = 1200.0,
            installmentCount = 6,
            installmentAmount = 200.0,
            paidCount = 0,
            startDate = "2024-06-15",
            planType = "FIXED_DURATION",
            initialInstallmentAmount = null,
        )

        val useCase = RecalculateAmexInstallmentPlanUseCase()
        val (_, newPayments) = useCase.execute(
            existingPlan = plan,
            existingPayments = payments,
            newStrategy = AmexInstallmentStrategy.FixedAmount(150.0),
            today = "2024-06-25",
            currentStatementPaymentDueDate = "2024-06-20",
        )

        assertTrue(newPayments.none { it.dueDate == "2024-06-15" })
        assertEquals("2024-07-15", newPayments.first().dueDate)
    }

    @Test
    fun `recalculate keeps paid installments untouched`() {
        val buildUseCase = BuildAmexScheduledPaymentsUseCase()
        val payments = buildUseCase.execute(
            planId = "plan1",
            totalAmount = 1200.0,
            strategy = AmexInstallmentStrategy.FixedDuration(6),
            startDate = "2024-06-15",
        ).mapIndexed { index, payment ->
            if (index < 2) payment.copy(status = "PAID") else payment
        }
        val plan = com.expense.management.data.AmexPagoFlexPlanEntity(
            id = "plan1",
            statementId = "stmt1",
            transactionId = "tx1",
            totalAmount = 1200.0,
            installmentCount = 6,
            installmentAmount = 200.0,
            paidCount = 2,
            startDate = "2024-06-15",
            planType = "FIXED_DURATION",
            initialInstallmentAmount = null,
        )

        val useCase = RecalculateAmexInstallmentPlanUseCase()
        val (updatedPlan, newPayments) = useCase.execute(
            existingPlan = plan,
            existingPayments = payments,
            newStrategy = AmexInstallmentStrategy.FixedAmount(200.0),
            today = "2024-08-10",
            currentStatementPaymentDueDate = "2024-08-20",
        )

        assertEquals(2 + newPayments.size, updatedPlan.installmentCount)
        assertEquals(1200.0 - 400.0, newPayments.sumOf { it.amount }, 0.01)
    }

    @Test
    fun `calculate outflow sums only pending target month payments`() {
        val payments = listOf(
            com.expense.management.data.AmexPagoFlexScheduledPaymentEntity(
                id = "p1",
                planId = "plan1",
                sequenceNumber = 1,
                dueDate = "2024-06-15",
                amount = 100.0,
                status = "PENDING",
            ),
            com.expense.management.data.AmexPagoFlexScheduledPaymentEntity(
                id = "p2",
                planId = "plan1",
                sequenceNumber = 2,
                dueDate = "2024-06-20",
                amount = 100.0,
                status = "PAID",
            ),
            com.expense.management.data.AmexPagoFlexScheduledPaymentEntity(
                id = "p3",
                planId = "plan1",
                sequenceNumber = 3,
                dueDate = "2024-07-15",
                amount = 100.0,
                status = "PENDING",
            ),
        )
        val useCase = CalculateAmexCurrentAccountOutflowUseCase()

        val outflow = useCase.execute("2024-06", payments, emptyList())

        assertEquals(100.0, outflow, 0.01)
    }
}
