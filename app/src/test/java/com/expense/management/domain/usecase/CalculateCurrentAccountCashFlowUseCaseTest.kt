package com.expense.management.domain.usecase

import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity
import com.expense.management.data.InstallmentScheduledPaymentEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.CurrentAccountCashFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth

class CalculateCurrentAccountCashFlowUseCaseTest {

    private val useCase = CalculateCurrentAccountCashFlowUseCase()
    private val month = YearMonth.of(2026, 6)

    private fun tx(id: String, type: TransactionType, amount: Double, isCreditCard: Boolean, effectiveDate: String) = TransactionEntity(
        id = id,
        date = effectiveDate,
        description = "test",
        amount = amount,
        categoryId = "cat",
        type = type,
        isCreditCard = isCreditCard,
        effectiveDate = effectiveDate,
        originalAmount = amount,
        originalCurrency = "EUR",
    )

    private fun amexPayment(id: String, dueDate: String, amount: Double, status: String, expenseTxId: String? = null) = AmexPagoFlexScheduledPaymentEntity(
        id = id,
        planId = "plan1",
        sequenceNumber = 1,
        dueDate = dueDate,
        amount = amount,
        status = status,
        expenseTransactionId = expenseTxId,
    )

    private fun genericPayment(id: String, dueDate: String, amount: Double, status: String, expenseTxId: String? = null) = InstallmentScheduledPaymentEntity(
        id = id,
        planId = "planG1",
        dueDate = dueDate,
        amount = amount,
        status = status,
        expenseTransactionId = expenseTxId,
    )

    @Test
    fun `income excludes credit card transactions`() {
        val transactions = listOf(
            tx("i1", TransactionType.INCOME, 1000.0, false, "2026-06-10"),
            tx("i2", TransactionType.INCOME, 200.0, true, "2026-06-10"),
        )
        val result = useCase.execute(transactions, emptyList(), emptyList(), month)
        assertEquals(1000.0, result.income, 0.01)
    }

    @Test
    fun `outflows sum non-cc expenses plus pending scheduled payments without expense tx`() {
        val transactions = listOf(
            tx("e1", TransactionType.EXPENSE, 300.0, false, "2026-06-05"),
            tx("e2", TransactionType.EXPENSE, 500.0, true, "2026-06-05"),
        )
        val amex = listOf(
            amexPayment("a1", "2026-06-15", 200.0, "PENDING", expenseTxId = null),
            amexPayment("a2", "2026-06-20", 200.0, "PENDING", expenseTxId = "existing"),
        )
        val generic = listOf(
            genericPayment("g1", "2026-06-25", 150.0, "PENDING", expenseTxId = null),
            genericPayment("g2", "2026-07-01", 150.0, "PENDING", expenseTxId = null),
        )
        val result = useCase.execute(transactions, amex, generic, month)
        assertEquals(300.0 + 200.0 + 150.0, result.outflows, 0.01)
    }

    @Test
    fun `paid scheduled payments are not double counted when expense tx exists`() {
        val transactions = listOf(
            tx("e1", TransactionType.EXPENSE, 200.0, false, "2026-06-15"),
        )
        val amex = listOf(
            amexPayment("a1", "2026-06-15", 200.0, "PAID", expenseTxId = "e1"),
        )
        val result = useCase.execute(transactions, amex, emptyList(), month)
        assertEquals(200.0, result.outflows, 0.01)
    }

    @Test
    fun `transactions outside target month are ignored`() {
        val transactions = listOf(
            tx("e1", TransactionType.EXPENSE, 300.0, false, "2026-05-31"),
            tx("e2", TransactionType.EXPENSE, 400.0, false, "2026-07-01"),
        )
        val result = useCase.execute(transactions, emptyList(), emptyList(), month)
        assertEquals(0.0, result.outflows, 0.01)
    }

    @Test
    fun `net balance equals income minus outflows`() {
        val transactions = listOf(
            tx("i1", TransactionType.INCOME, 1000.0, false, "2026-06-10"),
            tx("e1", TransactionType.EXPENSE, 300.0, false, "2026-06-05"),
        )
        val result: CurrentAccountCashFlow = useCase.execute(transactions, emptyList(), emptyList(), month)
        assertEquals(700.0, result.net, 0.01)
    }
}
