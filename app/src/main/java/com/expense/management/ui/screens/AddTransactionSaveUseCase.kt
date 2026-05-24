package com.expense.management.ui.screens

import com.expense.management.data.CategoryEntity
import com.expense.management.data.CreditCardEntity
import com.expense.management.data.RecurrenceType
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.utils.DateUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.floor

sealed interface AddTransactionSaveResult {
    data class Ready(val transactions: List<TransactionEntity>) : AddTransactionSaveResult
    data object PreviousMonthWarning : AddTransactionSaveResult
    data class Error(val message: String) : AddTransactionSaveResult
}

class AddTransactionSaveUseCase {

    operator fun invoke(
        uiState: AddTransactionUiState,
        transactionToEdit: TransactionEntity?,
        availableCategories: List<CategoryEntity>,
        availableCreditCards: List<CreditCardEntity>,
        isCC: Boolean,
        dateFormat: String,
        locale: java.util.Locale,
        installmentLabel: String = "installment",
    ): AddTransactionSaveResult {
        val displayFormatter = DateTimeFormatter.ofPattern(dateFormat)
        val amount = uiState.amountText.toDoubleOrNull() ?: 0.0
        val originalAmount = uiState.originalAmountText.toDoubleOrNull() ?: amount

        if (amount <= 0 || uiState.description.isBlank()) {
            return AddTransactionSaveResult.Error("error_invalid_input")
        }

        val transactionDate = try {
            LocalDate.parse(uiState.dateStr, displayFormatter)
        } catch (e: DateTimeParseException) {
            return AddTransactionSaveResult.Error("error_invalid_date_format")
        }

        val limitMonth = YearMonth.now().minusMonths(1)
        val transactionMonth = YearMonth.from(transactionDate)

        if (transactionMonth.isBefore(limitMonth)) {
            val formattedMonth = limitMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
            return AddTransactionSaveResult.Error("error_past_limit_date:$formattedMonth")
        }

        if (transactionMonth.isBefore(YearMonth.now()) && transactionToEdit == null && !uiState.ignoreDateWarning) {
            return AddTransactionSaveResult.PreviousMonthWarning
        }

        val dateToSave = transactionDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val transactionId = transactionToEdit?.id ?: UUID.randomUUID().toString()
        val transactions = mutableListOf<TransactionEntity>()

        if (uiState.isInstallment && transactionToEdit == null) {
            val installmentAmountFromField = uiState.installmentAmountText.toDoubleOrNull() ?: 0.0

            val finalInstallmentsCount = if (uiState.calculationMode == "amount" && installmentAmountFromField > 0) {
                ceil(amount / installmentAmountFromField).toInt()
            } else {
                uiState.installmentsCount
            }

            if (finalInstallmentsCount <= 0) {
                return AddTransactionSaveResult.Error("error_invalid_input")
            }

            val groupId = UUID.randomUUID().toString()
            val startInstallmentDate = try {
                LocalDate.parse(uiState.installmentStartDateStr, displayFormatter)
            } catch (e: DateTimeParseException) {
                transactionDate
            }

            for (i in 0 until finalInstallmentsCount) {
                val installmentDate = startInstallmentDate.plusMonths(i.toLong())
                val selectedCard = availableCreditCards.find { it.id == uiState.creditCardId }
                val settlementDate = if (uiState.isCreditCard && selectedCard != null) {
                    DateUtils.calculateEffectiveDate(installmentDate, selectedCard)
                } else {
                    installmentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                }

                val effectiveDate = if (uiState.isCreditCard && uiState.type == TransactionType.INCOME) {
                    installmentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                } else if (uiState.isCreditCard && uiState.applyCcDelayToInstallments) {
                    settlementDate
                } else {
                    installmentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                }

                val newId = if (i == 0) transactionId else UUID.randomUUID().toString()
                val installmentDateToSave = installmentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                val currentInstallmentAmount: Double
                val currentOriginalInstallmentAmount: Double
                val originalRatio = if (amount > 0) originalAmount / amount else 1.0

                if (uiState.calculationMode == "amount" && installmentAmountFromField > 0) {
                    currentInstallmentAmount = if (i < finalInstallmentsCount - 1) {
                        installmentAmountFromField
                    } else {
                        amount - (installmentAmountFromField * (finalInstallmentsCount - 1))
                    }
                    currentOriginalInstallmentAmount = currentInstallmentAmount * originalRatio
                } else {
                    val regularInstallment = floor((amount / finalInstallmentsCount) * 100) / 100
                    currentInstallmentAmount = if (i < finalInstallmentsCount - 1) {
                        regularInstallment
                    } else {
                        amount - (regularInstallment * (finalInstallmentsCount - 1))
                    }

                    val regularOriginalInstallment = floor((originalAmount / finalInstallmentsCount) * 100) / 100
                    currentOriginalInstallmentAmount = if (i < finalInstallmentsCount - 1) {
                        regularOriginalInstallment
                    } else {
                        originalAmount - (regularOriginalInstallment * (finalInstallmentsCount - 1))
                    }
                }

                if (uiState.isCreditCard && uiState.type == TransactionType.INCOME) {
                    val expenseCategoryId = availableCategories.find { it.id == "credit_card_payment" }?.id
                        ?: availableCategories.firstOrNull { it.type == TransactionType.EXPENSE }?.id
                        ?: "other"

                    transactions.add(
                        TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            date = installmentDateToSave,
                            description = "[${selectedCard?.name ?: "Credit Card"}] ${uiState.description.trim()} ($installmentLabel ${i + 1}/$finalInstallmentsCount) (Future Payment)",
                            amount = currentInstallmentAmount,
                            categoryId = expenseCategoryId,
                            type = TransactionType.EXPENSE,
                            isCreditCard = false,
                            originalAmount = currentOriginalInstallmentAmount,
                            originalCurrency = uiState.originalCurrency,
                            effectiveDate = settlementDate,
                            creditCardId = null,
                            groupId = groupId,
                        ),
                    )
                }

                transactions.add(
                    TransactionEntity(
                        id = newId,
                        date = installmentDateToSave,
                        description = "${uiState.description} ($installmentLabel ${i + 1}/$finalInstallmentsCount)",
                        amount = currentInstallmentAmount,
                        categoryId = uiState.selectedCategory,
                        type = uiState.type,
                        isCreditCard = uiState.isCreditCard,
                        originalAmount = currentOriginalInstallmentAmount,
                        originalCurrency = uiState.originalCurrency,
                        effectiveDate = effectiveDate,
                        installmentNumber = i + 1,
                        totalInstallments = finalInstallmentsCount,
                        groupId = groupId,
                        creditCardId = uiState.creditCardId,
                    ),
                )
            }
        } else if (uiState.recurrenceType != RecurrenceType.NONE && transactionToEdit == null) {
            val groupId = UUID.randomUUID().toString()
            var currentOccurrenceDate = transactionDate

            for (count in 0 until uiState.recurrenceLimit) {
                val selectedCard = availableCreditCards.find { it.id == uiState.creditCardId }
                val settlementDate = if (uiState.isCreditCard && selectedCard != null) {
                    DateUtils.calculateEffectiveDate(currentOccurrenceDate, selectedCard)
                } else {
                    currentOccurrenceDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                }

                val effectiveDate = if (uiState.isCreditCard && uiState.type == TransactionType.INCOME) {
                    currentOccurrenceDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                } else {
                    settlementDate
                }

                val newId = if (count == 0) transactionId else UUID.randomUUID().toString()
                val occurrenceDateStr = currentOccurrenceDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                if (uiState.isCreditCard && uiState.type == TransactionType.INCOME) {
                    val expenseCategoryId = availableCategories.find { it.id == "credit_card_payment" }?.id
                        ?: availableCategories.firstOrNull { it.type == TransactionType.EXPENSE }?.id
                        ?: "other"

                    transactions.add(
                        TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            date = occurrenceDateStr,
                            description = "[${selectedCard?.name ?: "Credit Card"}] ${uiState.description.trim()} (Future Payment)",
                            amount = amount,
                            categoryId = expenseCategoryId,
                            type = TransactionType.EXPENSE,
                            isCreditCard = false,
                            originalAmount = originalAmount,
                            originalCurrency = uiState.originalCurrency,
                            effectiveDate = settlementDate,
                            creditCardId = null,
                            groupId = groupId,
                        ),
                    )
                }

                transactions.add(
                    TransactionEntity(
                        id = newId,
                        date = occurrenceDateStr,
                        description = uiState.description.trim(),
                        amount = amount,
                        categoryId = uiState.selectedCategory,
                        type = uiState.type,
                        isCreditCard = uiState.isCreditCard,
                        originalAmount = originalAmount,
                        originalCurrency = uiState.originalCurrency,
                        effectiveDate = effectiveDate,
                        groupId = groupId,
                        creditCardId = if (uiState.isCreditCard) uiState.creditCardId else null,
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
        } else {
            val selectedCard = availableCreditCards.find { it.id == uiState.creditCardId }
            if (selectedCard == null && uiState.isCreditCard) {
                return AddTransactionSaveResult.Error("error_no_card_selected")
            }

            val settlementDate = if (uiState.isCreditCard && selectedCard != null) {
                DateUtils.calculateEffectiveDate(transactionDate, selectedCard)
            } else {
                transactionDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            }

            val effectiveDate = if (uiState.isCreditCard && uiState.type == TransactionType.INCOME) {
                dateToSave
            } else {
                settlementDate
            }

            val commonGroupId = if (uiState.isCreditCard && !uiState.isInstallment && transactionToEdit == null) {
                UUID.randomUUID().toString()
            } else {
                transactionToEdit?.groupId
            }

            if (uiState.isCreditCard && !uiState.isInstallment && transactionToEdit == null) {
                if (uiState.type == TransactionType.INCOME) {
                    val expenseCategoryId = availableCategories.find { it.id == "credit_card_payment" }?.id
                        ?: availableCategories.firstOrNull { it.type == TransactionType.EXPENSE }?.id
                        ?: "other"

                    transactions.add(
                        TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            date = dateToSave,
                            description = "[${selectedCard?.name ?: "Credit Card"}] ${uiState.description.trim()} (Future Payment)",
                            amount = amount,
                            categoryId = expenseCategoryId,
                            type = TransactionType.EXPENSE,
                            isCreditCard = false,
                            originalAmount = originalAmount,
                            originalCurrency = uiState.originalCurrency,
                            effectiveDate = settlementDate,
                            creditCardId = null,
                            groupId = commonGroupId,
                        ),
                    )
                }
            }

            transactions.add(
                TransactionEntity(
                    id = transactionId,
                    date = dateToSave,
                    description = uiState.description.trim(),
                    amount = amount,
                    categoryId = uiState.selectedCategory,
                    type = uiState.type,
                    isCreditCard = uiState.isCreditCard,
                    originalAmount = originalAmount,
                    originalCurrency = uiState.originalCurrency,
                    effectiveDate = effectiveDate,
                    installmentNumber = transactionToEdit?.installmentNumber,
                    totalInstallments = transactionToEdit?.totalInstallments,
                    groupId = commonGroupId,
                    creditCardId = if (uiState.isCreditCard) uiState.creditCardId else null,
                    recurrenceType = transactionToEdit?.recurrenceType ?: RecurrenceType.NONE,
                    recurrenceLimit = transactionToEdit?.recurrenceLimit,
                ),
            )
        }

        return AddTransactionSaveResult.Ready(transactions)
    }
}
