package com.expense.management.utils

import com.expense.management.data.CategoryEntity
import com.expense.management.data.CreditCardEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class TransactionLogicTest {

    private val categories = listOf(
        CategoryEntity("credit_card_adjustment", "Credito", "icon", TransactionType.INCOME),
        CategoryEntity("credit_card_payment", "Addebito", "icon", TransactionType.EXPENSE),
        CategoryEntity("other", "Altro", "icon", TransactionType.EXPENSE),
    )

    @Test
    fun `prepareCreditCardTransactions should create expense settlement for income top-up`() {
        // Given
        val groupId = UUID.randomUUID().toString()
        val mainIncome = TransactionEntity(
            id = "1",
            date = "2024-05-15",
            description = "Top-up",
            amount = 100.0,
            categoryId = "recharge",
            type = TransactionType.INCOME,
            isCreditCard = true,
            effectiveDate = "2024-06-15",
            originalAmount = 100.0,
            originalCurrency = "EUR",
            groupId = groupId,
        )

        val card = mockk<CreditCardEntity> {
            every { name } returns "Visa"
        }

        // When
        val results = TransactionLogic.prepareCreditCardTransactions(mainIncome, card, categories)

        // Then
        assertEquals(2, results.size)
        val technical = results.find { it.type == TransactionType.EXPENSE }
        val main = results.find { it.type == TransactionType.INCOME }

        assertEquals("2024-06-15", technical?.effectiveDate)
        assertEquals("2024-05-15", main?.date)
    }
}
