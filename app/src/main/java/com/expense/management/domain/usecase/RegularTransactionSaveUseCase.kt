package com.expense.management.domain.usecase

import com.expense.management.data.CategoryEntity
import com.expense.management.data.RecurrenceType
import com.expense.management.data.TransactionEntity
import com.expense.management.ui.screens.RegularTransactionUiState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.UUID

sealed interface RegularTransactionSaveResult {
    data class Ready(val transactions: List<TransactionEntity>) : RegularTransactionSaveResult
    data object PreviousMonthWarning : RegularTransactionSaveResult
    data class Error(val message: String) : RegularTransactionSaveResult
}

class RegularTransactionSaveUseCase {

    operator fun invoke(
        uiState: RegularTransactionUiState,
        transactionToEdit: TransactionEntity?,
        availableCategories: List<CategoryEntity>,
        dateFormat: String,
        locale: Locale,
    ): RegularTransactionSaveResult {
        val displayFormatter = DateTimeFormatter.ofPattern(dateFormat)
        val amount = uiState.amountText.toDoubleOrNull() ?: 0.0
        val originalAmount = uiState.originalAmountText.toDoubleOrNull() ?: amount

        if (amount <= 0 || uiState.description.isBlank()) {
            return RegularTransactionSaveResult.Error("error_invalid_input")
        }

        val transactionDate = try {
            LocalDate.parse(uiState.dateStr, displayFormatter)
        } catch (e: DateTimeParseException) {
            return RegularTransactionSaveResult.Error("error_invalid_date_format")
        }

        val limitMonth = YearMonth.now().minusMonths(1)
        val transactionMonth = YearMonth.from(transactionDate)

        if (transactionMonth.isBefore(limitMonth)) {
            val formattedMonth = limitMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
            return RegularTransactionSaveResult.Error("error_past_limit_date:$formattedMonth")
        }

        if (transactionMonth.isBefore(YearMonth.now()) && transactionToEdit == null && !uiState.ignoreDateWarning) {
            return RegularTransactionSaveResult.PreviousMonthWarning
        }

        val dateToSave = transactionDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val transactionId = transactionToEdit?.id ?: UUID.randomUUID().toString()

        val transactions = if (uiState.recurrenceType != RecurrenceType.NONE && transactionToEdit == null) {
            buildRecurrenceTransactions(
                uiState = uiState,
                availableCategories = availableCategories,
                transactionDate = transactionDate,
                amount = amount,
                originalAmount = originalAmount,
                transactionId = transactionId,
            )
        } else {
            listOf(
                TransactionEntity(
                    id = transactionId,
                    date = dateToSave,
                    description = uiState.description.trim(),
                    amount = amount,
                    categoryId = uiState.selectedCategory,
                    type = uiState.type,
                    isCreditCard = false,
                    originalAmount = originalAmount,
                    originalCurrency = uiState.originalCurrency,
                    effectiveDate = dateToSave,
                    installmentNumber = transactionToEdit?.installmentNumber,
                    totalInstallments = transactionToEdit?.totalInstallments,
                    groupId = transactionToEdit?.groupId,
                    creditCardId = null,
                    paymentMethodId = if (uiState.isPaymentMethodEnabled) uiState.selectedPaymentMethodId else null,
                    recurrenceType = transactionToEdit?.recurrenceType ?: RecurrenceType.NONE,
                    recurrenceLimit = transactionToEdit?.recurrenceLimit,
                ),
            )
        }

        return RegularTransactionSaveResult.Ready(transactions)
    }

    private fun buildRecurrenceTransactions(
        uiState: RegularTransactionUiState,
        availableCategories: List<CategoryEntity>,
        transactionDate: LocalDate,
        amount: Double,
        originalAmount: Double,
        transactionId: String,
    ): List<TransactionEntity> {
        val groupId = UUID.randomUUID().toString()
        val result = mutableListOf<TransactionEntity>()
        var currentOccurrenceDate = transactionDate

        for (count in 0 until uiState.recurrenceLimit) {
            val newId = if (count == 0) transactionId else UUID.randomUUID().toString()
            val occurrenceDateStr = currentOccurrenceDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

            result.add(
                TransactionEntity(
                    id = newId,
                    date = occurrenceDateStr,
                    description = uiState.description.trim(),
                    amount = amount,
                    categoryId = uiState.selectedCategory,
                    type = uiState.type,
                    isCreditCard = false,
                    originalAmount = originalAmount,
                    originalCurrency = uiState.originalCurrency,
                    effectiveDate = occurrenceDateStr,
                    groupId = groupId,
                    creditCardId = null,
                    paymentMethodId = if (uiState.isPaymentMethodEnabled) uiState.selectedPaymentMethodId else null,
                    recurrenceType = uiState.recurrenceType,
                    recurrenceLimit = uiState.recurrenceLimit,
                ),
            )

            currentOccurrenceDate = when (uiState.recurrenceType) {
                RecurrenceType.DAILY -> currentOccurrenceDate.plusDays(1)
                RecurrenceType.WEEKLY -> currentOccurrenceDate.plusWeeks(1)
                RecurrenceType.MONTHLY -> currentOccurrenceDate.plusMonths(1)
                RecurrenceType.YEARLY -> currentOccurrenceDate.plusYears(1)
                else -> break
            }
        }
        return result
    }
}
