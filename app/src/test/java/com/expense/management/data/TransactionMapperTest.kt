package com.expense.management.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Mapper delle transazioni")
class TransactionMapperTest {

    @Test
    fun `entity to domain and back keeps every field`() {
        // Ogni campo valorizzato: un campo nuovo dimenticato nel dominio torna al default e il test fallisce
        val entity = TransactionEntity(
            id = "tx1",
            date = "2026-10-28",
            description = "Addebito Visa",
            amount = 180.0,
            categoryId = "credit_card_payment",
            type = TransactionType.EXPENSE,
            isCreditCard = true,
            effectiveDate = "2026-11-15",
            originalAmount = 175.5,
            originalCurrency = "USD",
            installmentNumber = 2,
            totalInstallments = 3,
            groupId = "group1",
            creditCardId = "visa1",
            paymentMethodId = "bank1",
            recurrenceType = RecurrenceType.MONTHLY,
            recurrenceEndDate = "2027-01-01",
            recurrenceLimit = 5,
            billingCycleId = "visa1:2026-10",
            sourceRef = "cycle:visa1:2026-10:settlement",
        )

        assertEquals(entity, entity.toDomain().toData())
    }
}
