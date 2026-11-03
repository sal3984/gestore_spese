package com.expense.management.domain.usecase

import com.expense.management.data.BackupData
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RestoreDataUseCase")
class RestoreDataUseCaseTest {

    @Test
    fun `restored transactions lose their billing cycle but keep their source ref`() = runBlocking {
        val repository = mockk<ExpenseRepository>(relaxed = true)
        coEvery { repository.withTransaction<Unit>(any()) } coAnswers { firstArg<suspend () -> Unit>().invoke() }
        val transaction = TransactionEntity(
            id = "tx1",
            date = "2025-05-10",
            description = "Spesa",
            amount = 50.0,
            categoryId = "shopping",
            type = TransactionType.EXPENSE,
            isCreditCard = true,
            effectiveDate = "2025-06-15",
            originalAmount = 50.0,
            originalCurrency = "EUR",
            paymentMethodId = "saldo1",
            billingCycleId = "saldo1:2025-05",
            sourceRef = "cycle:saldo1:2025-05:settlement",
        )
        val restored = slot<List<TransactionEntity>>()

        RestoreDataUseCase(repository)(BackupData(transactions = listOf(transaction), categories = emptyList(), creditCard = null))

        coVerify { repository.insertAllTransactions(capture(restored)) }
        assertNull(restored.captured.single().billingCycleId)
        assertEquals("cycle:saldo1:2025-05:settlement", restored.captured.single().sourceRef)
    }
}
