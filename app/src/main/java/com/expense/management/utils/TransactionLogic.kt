package com.expense.management.utils

import com.expense.management.data.CategoryEntity
import com.expense.management.data.CreditCardEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import java.util.UUID

object TransactionLogic {

    fun prepareCreditCardTransactions(
        mainTransaction: TransactionEntity,
        selectedCard: CreditCardEntity?,
        availableCategories: List<CategoryEntity>,
    ): List<TransactionEntity> {
        if (mainTransaction.isCreditCard && (mainTransaction.totalInstallments ?: 1) <= 1 && mainTransaction.groupId != null) {
            val transactions = mutableListOf<TransactionEntity>()
            val dateToSave = mainTransaction.date
            val settlementDate = mainTransaction.effectiveDate

            if (mainTransaction.type == TransactionType.EXPENSE) {
                // SPESA: Income now
                val incomeCategoryId = availableCategories.find { it.id == "credit_card_adjustment" }?.id
                    ?: availableCategories.firstOrNull { it.type == TransactionType.INCOME }?.id
                    ?: "salary"

                transactions.add(
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        date = dateToSave,
                        description = "[${selectedCard?.name ?: "Credit Card"}] ${mainTransaction.description}",
                        amount = mainTransaction.amount,
                        categoryId = incomeCategoryId,
                        type = TransactionType.INCOME,
                        isCreditCard = false,
                        originalAmount = mainTransaction.originalAmount,
                        originalCurrency = mainTransaction.originalCurrency,
                        effectiveDate = dateToSave,
                        creditCardId = null,
                        groupId = mainTransaction.groupId,
                    ),
                )
            } else if (mainTransaction.type == TransactionType.INCOME) {
                // RICARICA: Expense later
                val expenseCategoryId = availableCategories.find { it.id == "credit_card_payment" }?.id
                    ?: availableCategories.firstOrNull { it.type == TransactionType.EXPENSE }?.id
                    ?: "other"

                transactions.add(
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        date = dateToSave,
                        description = "[${selectedCard?.name ?: "Credit Card"}] ${mainTransaction.description}",
                        amount = mainTransaction.amount,
                        categoryId = expenseCategoryId,
                        type = TransactionType.EXPENSE,
                        isCreditCard = false,
                        originalAmount = mainTransaction.originalAmount,
                        originalCurrency = mainTransaction.originalCurrency,
                        effectiveDate = settlementDate,
                        creditCardId = null,
                        groupId = mainTransaction.groupId,
                    ),
                )
            }
            transactions.add(mainTransaction)
            return transactions
        }
        return listOf(mainTransaction)
    }
}
