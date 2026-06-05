package com.expense.management.domain.usecase

import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.ActiveCreditCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class GenerateCreditCardPaymentUseCase {

    fun execute(
        card: ActiveCreditCard,
        paymentAmount: Double,
        paymentDate: LocalDate,
        categories: List<com.expense.management.data.CategoryEntity>,
    ): List<TransactionEntity> {
        val dateStr = paymentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val groupId = UUID.randomUUID().toString()
        val expenseCategoryId = categories.find { it.id == "credit_card_payment" }?.id
            ?: categories.firstOrNull { it.type == TransactionType.EXPENSE }?.id
            ?: "other"
        val incomeCategoryId = categories.find { it.id == "credit_card_adjustment" }?.id
            ?: categories.firstOrNull { it.type == TransactionType.INCOME }?.id
            ?: "other"
        val isRevolving = card.cardType == com.expense.management.domain.model.CreditCardType.REVOLVING
        val paymentLabel = if (isRevolving) "Rata" else "Addebito"
        val restorationLabel = if (isRevolving) "Ripristino plafond" else "Ripristino plafond"

        val baseId = UUID.randomUUID().toString()
        val bankDebit = TransactionEntity(
            id = baseId,
            date = dateStr,
            description = "$paymentLabel ${card.name}",
            amount = paymentAmount,
            categoryId = expenseCategoryId,
            type = TransactionType.EXPENSE,
            isCreditCard = false,
            effectiveDate = dateStr,
            originalAmount = paymentAmount,
            originalCurrency = "",
            creditCardId = null,
            paymentMethodId = null,
            groupId = groupId,
        )

        val creditRestoration = TransactionEntity(
            id = "${baseId}_restore",
            date = dateStr,
            description = "[${card.name}] $restorationLabel",
            amount = paymentAmount,
            categoryId = incomeCategoryId,
            type = TransactionType.INCOME,
            isCreditCard = true,
            effectiveDate = dateStr,
            originalAmount = paymentAmount,
            originalCurrency = "",
            creditCardId = card.id,
            paymentMethodId = card.id,
            groupId = groupId,
        )

        return listOf(bankDebit, creditRestoration)
    }
}
