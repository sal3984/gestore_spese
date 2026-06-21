package com.expense.management.domain.usecase

import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.RecurrenceType
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.DeleteType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DeleteTransactionUseCase")
class DeleteTransactionUseCaseTest {

    private lateinit var repository: ExpenseRepository
    private lateinit var useCase: DeleteTransactionUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = DeleteTransactionUseCase(repository)
        coEvery { repository.getAmexScheduledPaymentByExpenseTxId(any()) } returns null
        coEvery { repository.getGenericScheduledPaymentByExpenseTxId(any()) } returns null
    }

    @Test
    fun `should delete single transaction when delete type is SINGLE`() {
        val transaction = TransactionEntity(
            id = "tx_1",
            date = "2024-06-15",
            description = "Test",
            amount = 100.0,
            categoryId = "food",
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = "2024-06-15",
            originalAmount = 100.0,
            originalCurrency = "",
        )
        coEvery { repository.getTransactionById("tx_1") } returns transaction
        coEvery { repository.deleteTransaction("tx_1") } returns Unit

        runBlocking { useCase("tx_1", DeleteType.SINGLE) }

        coVerify(exactly = 1) { repository.deleteTransaction("tx_1") }
        coVerify(exactly = 0) { repository.deleteTransactionsByIds(any()) }
    }

    @Test
    fun `should return early when transaction not found`() {
        coEvery { repository.getTransactionById("tx_unknown") } returns null

        runBlocking { useCase("tx_unknown", DeleteType.SINGLE) }

        coVerify(exactly = 0) { repository.deleteTransaction(any()) }
        coVerify(exactly = 0) { repository.deleteTransactionsByIds(any()) }
    }

    @Test
    fun `should delete this and subsequent transactions in group`() {
        val transaction = TransactionEntity(
            id = "tx_2",
            date = "2024-06-15",
            description = "Test",
            amount = 100.0,
            categoryId = "food",
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = "2024-06-15",
            originalAmount = 100.0,
            originalCurrency = "",
            groupId = "group_1",
            recurrenceType = RecurrenceType.MONTHLY,
        )
        val groupTransactions = listOf(
            TransactionEntity(
                id = "tx_1",
                date = "2024-05-15",
                description = "First",
                amount = 100.0,
                categoryId = "food",
                type = TransactionType.EXPENSE,
                isCreditCard = false,
                effectiveDate = "2024-05-15",
                originalAmount = 100.0,
                originalCurrency = "",
                groupId = "group_1",
            ),
            transaction,
            TransactionEntity(
                id = "tx_3",
                date = "2024-07-15",
                description = "Third",
                amount = 100.0,
                categoryId = "food",
                type = TransactionType.EXPENSE,
                isCreditCard = false,
                effectiveDate = "2024-07-15",
                originalAmount = 100.0,
                originalCurrency = "",
                groupId = "group_1",
            ),
        )
        coEvery { repository.getTransactionById("tx_2") } returns transaction
        coEvery { repository.getTransactionsByGroupId("group_1") } returns groupTransactions
        coEvery { repository.deleteTransactionsByIds(any()) } returns Unit

        runBlocking { useCase("tx_2", DeleteType.THIS_AND_SUBSEQUENT) }

        coVerify(exactly = 1) { repository.deleteTransactionsByIds(listOf("tx_2", "tx_3")) }
        coVerify(exactly = 0) { repository.deleteTransaction(any()) }
    }

    @Test
    fun `should delete single when THIS_AND_SUBSEQUENT but no groupId`() {
        val transaction = TransactionEntity(
            id = "tx_1",
            date = "2024-06-15",
            description = "Test",
            amount = 100.0,
            categoryId = "food",
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = "2024-06-15",
            originalAmount = 100.0,
            originalCurrency = "",
            groupId = null,
        )
        coEvery { repository.getTransactionById("tx_1") } returns transaction
        coEvery { repository.deleteTransaction("tx_1") } returns Unit

        runBlocking { useCase("tx_1", DeleteType.THIS_AND_SUBSEQUENT) }

        coVerify(exactly = 1) { repository.deleteTransaction("tx_1") }
        coVerify(exactly = 0) { repository.deleteTransactionsByIds(any()) }
    }

    @Test
    fun `should delete single when THIS_AND_SUBSEQUENT but recurrence is NONE`() {
        val transaction = TransactionEntity(
            id = "tx_1",
            date = "2024-06-15",
            description = "Test",
            amount = 100.0,
            categoryId = "food",
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = "2024-06-15",
            originalAmount = 100.0,
            originalCurrency = "",
            groupId = "group_1",
            recurrenceType = RecurrenceType.NONE,
        )
        coEvery { repository.getTransactionById("tx_1") } returns transaction
        coEvery { repository.deleteTransaction("tx_1") } returns Unit

        runBlocking { useCase("tx_1", DeleteType.THIS_AND_SUBSEQUENT) }

        coVerify(exactly = 1) { repository.deleteTransaction("tx_1") }
        coVerify(exactly = 0) { repository.deleteTransactionsByIds(any()) }
    }

    @Test
    fun `should revert amex scheduled payment to pending when linked transaction is deleted`() {
        val transaction = TransactionEntity(
            id = "tx_amex_debit",
            date = "2024-06-15",
            description = "Rata Amex 1/6",
            amount = 200.0,
            categoryId = "credit_card_payment",
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = "2024-06-15",
            originalAmount = 200.0,
            originalCurrency = "€",
        )
        val scheduledPayment = AmexPagoFlexScheduledPaymentEntity(
            id = "sp_1",
            planId = "plan_1",
            sequenceNumber = 1,
            dueDate = "2024-06-15",
            amount = 200.0,
            status = "PAID",
            expenseTransactionId = "tx_amex_debit",
        )
        val plan = AmexPagoFlexPlanEntity(
            id = "plan_1",
            statementId = "stmt_1",
            transactionId = "orig_tx",
            totalAmount = 1200.0,
            installmentCount = 6,
            installmentAmount = 200.0,
            paidCount = 1,
            startDate = "2024-06-15",
        )
        coEvery { repository.getTransactionById("tx_amex_debit") } returns transaction
        coEvery { repository.deleteTransaction("tx_amex_debit") } returns Unit
        coEvery { repository.getAmexScheduledPaymentByExpenseTxId("tx_amex_debit") } returns scheduledPayment
        coEvery { repository.getAmexPagoFlexPlanById("plan_1") } returns plan
        coEvery { repository.getAmexScheduledPaymentsForPlan("plan_1") } returns listOf(scheduledPayment.copy(status = "PENDING", expenseTransactionId = null))
        coEvery { repository.updateAmexPagoFlexPaidCount(any(), any()) } returns Unit
        coEvery { repository.revertAmexScheduledPaymentToPending(any()) } returns Unit

        runBlocking { useCase("tx_amex_debit", DeleteType.SINGLE) }

        coVerify(exactly = 1) { repository.revertAmexScheduledPaymentToPending("sp_1") }
        coVerify(exactly = 1) { repository.updateAmexPagoFlexPaidCount("plan_1", 0) }
    }
}
