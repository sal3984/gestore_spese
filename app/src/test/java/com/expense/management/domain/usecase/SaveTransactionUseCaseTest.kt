package com.expense.management.domain.usecase

import com.expense.management.data.ExpenseRepository
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("SaveTransactionUseCase")
class SaveTransactionUseCaseTest {

    private lateinit var repository: ExpenseRepository
    private lateinit var useCase: SaveTransactionUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = SaveTransactionUseCase(repository)
    }

    @Test
    fun `should insert transaction when no existing transaction found`() {
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
        coEvery { repository.getTransactionById("tx_1") } returns null
        coEvery { repository.insertTransaction(any()) } returns Unit

        runBlocking { useCase(transaction) }

        coVerify(exactly = 1) { repository.insertTransaction(transaction) }
        coVerify(exactly = 0) { repository.getTransactionsByGroupId(any()) }
        coVerify(exactly = 0) { repository.updateTransactionsCategory(any()) }
    }

    @Test
    fun `should insert transaction when existing has no groupId`() {
        val existing = TransactionEntity(
            id = "tx_1",
            date = "2024-06-15",
            description = "Old",
            amount = 100.0,
            categoryId = "food",
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = "2024-06-15",
            originalAmount = 100.0,
            originalCurrency = "",
            groupId = null,
        )
        val updated = existing.copy(description = "Updated desc")
        coEvery { repository.getTransactionById("tx_1") } returns existing
        coEvery { repository.insertTransaction(any()) } returns Unit

        runBlocking { useCase(updated) }

        coVerify(exactly = 1) { repository.insertTransaction(updated) }
        coVerify(exactly = 0) { repository.getTransactionsByGroupId(any()) }
        coVerify(exactly = 0) { repository.updateTransactionsCategory(any()) }
    }

    @Test
    fun `should insert transaction when group exists but totalInstallments is 1`() {
        val existing = TransactionEntity(
            id = "tx_1",
            date = "2024-06-15",
            description = "Old",
            amount = 100.0,
            categoryId = "food",
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = "2024-06-15",
            originalAmount = 100.0,
            originalCurrency = "",
            groupId = "group_1",
            totalInstallments = 1,
        )
        val updated = existing.copy(description = "Updated")
        coEvery { repository.getTransactionById("tx_1") } returns existing
        coEvery { repository.insertTransaction(any()) } returns Unit

        runBlocking { useCase(updated) }

        coVerify(exactly = 1) { repository.insertTransaction(updated) }
        coVerify(exactly = 0) { repository.getTransactionsByGroupId(any()) }
        coVerify(exactly = 0) { repository.updateTransactionsCategory(any()) }
    }

    @Test
    fun `should update all group transactions when category changed and group exists`() {
        val existing = TransactionEntity(
            id = "tx_1",
            date = "2024-06-15",
            description = "Old",
            amount = 100.0,
            categoryId = "food",
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = "2024-06-15",
            originalAmount = 100.0,
            originalCurrency = "",
            groupId = "group_1",
            totalInstallments = 3,
        )
        val updated = existing.copy(categoryId = "transport")
        val groupTransactions = listOf(
            existing,
            TransactionEntity(
                id = "tx_2",
                date = "2024-07-15",
                description = "Installment 2",
                amount = 100.0,
                categoryId = "food",
                type = TransactionType.EXPENSE,
                isCreditCard = false,
                effectiveDate = "2024-07-15",
                originalAmount = 100.0,
                originalCurrency = "",
                groupId = "group_1",
                totalInstallments = 3,
                installmentNumber = 2,
            ),
            TransactionEntity(
                id = "tx_3",
                date = "2024-08-15",
                description = "Installment 3",
                amount = 100.0,
                categoryId = "food",
                type = TransactionType.EXPENSE,
                isCreditCard = false,
                effectiveDate = "2024-08-15",
                originalAmount = 100.0,
                originalCurrency = "",
                groupId = "group_1",
                totalInstallments = 3,
                installmentNumber = 3,
            ),
        )
        val expectedUpdatedGroup = groupTransactions.map { it.copy(categoryId = "transport") }
        coEvery { repository.getTransactionById("tx_1") } returns existing
        coEvery { repository.getTransactionsByGroupId("group_1") } returns groupTransactions
        coEvery { repository.updateTransactionsCategory(any()) } returns Unit

        runBlocking { useCase(updated) }

        coVerify(exactly = 0) { repository.insertTransaction(any()) }
        coVerify(exactly = 1) { repository.getTransactionsByGroupId("group_1") }
        coVerify(exactly = 1) { repository.updateTransactionsCategory(expectedUpdatedGroup) }
    }

    @Test
    fun `should insert single transaction when category unchanged despite group`() {
        val existing = TransactionEntity(
            id = "tx_1",
            date = "2024-06-15",
            description = "Old",
            amount = 100.0,
            categoryId = "food",
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = "2024-06-15",
            originalAmount = 100.0,
            originalCurrency = "",
            groupId = "group_1",
            totalInstallments = 3,
        )
        val updated = existing.copy(description = "Same category updated")
        coEvery { repository.getTransactionById("tx_1") } returns existing
        coEvery { repository.insertTransaction(any()) } returns Unit

        runBlocking { useCase(updated) }

        coVerify(exactly = 1) { repository.insertTransaction(updated) }
        coVerify(exactly = 0) { repository.getTransactionsByGroupId(any()) }
        coVerify(exactly = 0) { repository.updateTransactionsCategory(any()) }
    }
}
