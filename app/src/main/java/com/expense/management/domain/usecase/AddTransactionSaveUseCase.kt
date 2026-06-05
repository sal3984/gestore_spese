package com.expense.management.domain.usecase

import com.expense.management.data.CategoryEntity
import com.expense.management.data.RecurrenceType
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.CreditCardType
import com.expense.management.ui.screens.AddTransactionUiState
import com.expense.management.utils.DateUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
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
        activeCreditCards: List<ActiveCreditCard>,
        dateFormat: String,
        locale: Locale,
        installmentLabel: String = "installment",
    ): AddTransactionSaveResult {
        val displayFormatter = DateTimeFormatter.ofPattern(dateFormat)
        val amount = uiState.amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
        val originalAmount = uiState.originalAmountText.replace(',', '.').toDoubleOrNull() ?: amount

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

        if (uiState.isInstallment && uiState.isCreditCard) {
            val selectedCard = activeCreditCards.find { it.id == uiState.creditCardId }
            if (selectedCard != null && selectedCard.cardType != CreditCardType.REVOLVING) {
                return AddTransactionSaveResult.Error("error_card_does_not_support_installments")
            }
        }

        val transactions = when {
            uiState.isInstallment && transactionToEdit == null -> {
                val installmentAmountFromField = uiState.installmentAmountText.replace(',', '.').toDoubleOrNull() ?: 0.0
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
                buildInstallmentTransactions(
                    uiState = uiState,
                    availableCategories = availableCategories,
                    activeCreditCards = activeCreditCards,
                    displayFormatter = displayFormatter,
                    amount = amount,
                    originalAmount = originalAmount,
                    transactionId = transactionId,
                    startInstallmentDate = startInstallmentDate,
                    finalInstallmentsCount = finalInstallmentsCount,
                    installmentAmountFromField = installmentAmountFromField,
                    groupId = groupId,
                    installmentLabel = installmentLabel,
                )
            }
            uiState.recurrenceType != RecurrenceType.NONE && transactionToEdit == null -> {
                val groupId = UUID.randomUUID().toString()
                buildRecurrenceTransactions(
                    uiState = uiState,
                    availableCategories = availableCategories,
                    activeCreditCards = activeCreditCards,
                    displayFormatter = displayFormatter,
                    transactionDate = transactionDate,
                    amount = amount,
                    originalAmount = originalAmount,
                    transactionId = transactionId,
                    groupId = groupId,
                )
            }
            else -> {
                val selectedCard = activeCreditCards.find { it.id == uiState.creditCardId }
                if (selectedCard == null && uiState.isCreditCard) {
                    return AddTransactionSaveResult.Error("error_no_card_selected")
                }
                val settlementDate = if (uiState.isCreditCard && selectedCard != null) {
                    DateUtils.calculateEffectiveDate(
                        transactionDate,
                        DateUtils.CardDateInfo(selectedCard.closingDay, selectedCard.paymentDay),
                    )
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
                buildSingleTransaction(
                    uiState = uiState,
                    availableCategories = availableCategories,
                    activeCreditCards = activeCreditCards,
                    dateToSave = dateToSave,
                    amount = amount,
                    originalAmount = originalAmount,
                    transactionId = transactionId,
                    transactionToEdit = transactionToEdit,
                    settlementDate = settlementDate,
                    effectiveDate = effectiveDate,
                    commonGroupId = commonGroupId,
                    selectedCard = selectedCard,
                )
            }
        }

        return AddTransactionSaveResult.Ready(transactions)
    }

    private fun buildInstallmentTransactions(
        uiState: AddTransactionUiState,
        availableCategories: List<CategoryEntity>,
        activeCreditCards: List<ActiveCreditCard>,
        displayFormatter: DateTimeFormatter,
        amount: Double,
        originalAmount: Double,
        transactionId: String,
        startInstallmentDate: LocalDate,
        finalInstallmentsCount: Int,
        installmentAmountFromField: Double,
        groupId: String,
        installmentLabel: String,
    ): List<TransactionEntity> {
        val result = mutableListOf<TransactionEntity>()
        val originalRatio = if (amount > 0) originalAmount / amount else 1.0

        for (i in 0 until finalInstallmentsCount) {
            val installmentDate = startInstallmentDate.plusMonths(i.toLong())
            val selectedCard = activeCreditCards.find { it.id == uiState.creditCardId }
            val settlementDate = if (uiState.isCreditCard && selectedCard != null) {
                DateUtils.calculateEffectiveDate(
                    installmentDate,
                    DateUtils.CardDateInfo(selectedCard.closingDay, selectedCard.paymentDay),
                )
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

            if (uiState.isCreditCard) {
                when (uiState.type) {
                    TransactionType.INCOME -> {
                        val expenseCategoryId = availableCategories.find { it.id == "credit_card_payment" }?.id
                            ?: availableCategories.firstOrNull { it.type == TransactionType.EXPENSE }?.id
                            ?: "other"
                        result.add(
                            TransactionEntity(
                                id = "${newId}_mirror",
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
                    TransactionType.EXPENSE -> {
                        val incomeCategoryId = availableCategories.find { it.id == "credit_card_adjustment" }?.id
                            ?: availableCategories.firstOrNull { it.type == TransactionType.INCOME }?.id
                            ?: "salary"
                        result.add(
                            TransactionEntity(
                                id = "${newId}_mirror",
                                date = installmentDateToSave,
                                description = "[${selectedCard?.name ?: "Credit Card"}] ${uiState.description.trim()} ($installmentLabel ${i + 1}/$finalInstallmentsCount)",
                                amount = currentInstallmentAmount,
                                categoryId = incomeCategoryId,
                                type = TransactionType.INCOME,
                                isCreditCard = false,
                                originalAmount = currentOriginalInstallmentAmount,
                                originalCurrency = uiState.originalCurrency,
                                effectiveDate = installmentDateToSave,
                                creditCardId = selectedCard?.id ?: uiState.creditCardId,
                                paymentMethodId = selectedCard?.id ?: uiState.selectedPaymentMethodId ?: uiState.creditCardId,
                                groupId = groupId,
                            ),
                        )
                    }
                }
            }

            result.add(
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
                    paymentMethodId = uiState.selectedPaymentMethodId ?: uiState.creditCardId,
                ),
            )
        }
        return result
    }

    private fun buildRecurrenceTransactions(
        uiState: AddTransactionUiState,
        availableCategories: List<CategoryEntity>,
        activeCreditCards: List<ActiveCreditCard>,
        displayFormatter: DateTimeFormatter,
        transactionDate: LocalDate,
        amount: Double,
        originalAmount: Double,
        transactionId: String,
        groupId: String,
    ): List<TransactionEntity> {
        val result = mutableListOf<TransactionEntity>()
        var currentOccurrenceDate = transactionDate

        for (count in 0 until uiState.recurrenceLimit) {
            val selectedCard = activeCreditCards.find { it.id == uiState.creditCardId }
            val settlementDate = if (uiState.isCreditCard && selectedCard != null) {
                DateUtils.calculateEffectiveDate(
                    currentOccurrenceDate,
                    DateUtils.CardDateInfo(selectedCard.closingDay, selectedCard.paymentDay),
                )
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

            if (uiState.isCreditCard) {
                when (uiState.type) {
                    TransactionType.INCOME -> {
                        val expenseCategoryId = availableCategories.find { it.id == "credit_card_payment" }?.id
                            ?: availableCategories.firstOrNull { it.type == TransactionType.EXPENSE }?.id
                            ?: "other"
                        result.add(
                            TransactionEntity(
                                id = "${newId}_mirror",
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
                                paymentMethodId = null,
                                groupId = groupId,
                            ),
                        )
                    }
                    TransactionType.EXPENSE -> {
                        val incomeCategoryId = availableCategories.find { it.id == "credit_card_adjustment" }?.id
                            ?: availableCategories.firstOrNull { it.type == TransactionType.INCOME }?.id
                            ?: "salary"
                        result.add(
                            TransactionEntity(
                                id = "${newId}_mirror",
                                date = occurrenceDateStr,
                                description = "[${selectedCard?.name ?: "Credit Card"}] ${uiState.description.trim()}",
                                amount = amount,
                                categoryId = incomeCategoryId,
                                type = TransactionType.INCOME,
                                isCreditCard = false,
                                originalAmount = originalAmount,
                                originalCurrency = uiState.originalCurrency,
                                effectiveDate = occurrenceDateStr,
                                creditCardId = selectedCard?.id ?: uiState.creditCardId,
                                paymentMethodId = selectedCard?.id ?: uiState.selectedPaymentMethodId ?: uiState.creditCardId,
                                groupId = groupId,
                            ),
                        )
                    }
                }
            }

            result.add(
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
                    paymentMethodId = uiState.selectedPaymentMethodId ?: uiState.creditCardId,
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

    private fun buildSingleTransaction(
        uiState: AddTransactionUiState,
        availableCategories: List<CategoryEntity>,
        activeCreditCards: List<ActiveCreditCard>,
        dateToSave: String,
        amount: Double,
        originalAmount: Double,
        transactionId: String,
        transactionToEdit: TransactionEntity?,
        settlementDate: String,
        effectiveDate: String,
        commonGroupId: String?,
        selectedCard: ActiveCreditCard?,
    ): List<TransactionEntity> {
        val result = mutableListOf<TransactionEntity>()

        if (uiState.isCreditCard && !uiState.isInstallment) {
            when (uiState.type) {
                TransactionType.INCOME -> {
                    val expenseCategoryId = availableCategories.find { it.id == "credit_card_payment" }?.id
                        ?: availableCategories.firstOrNull { it.type == TransactionType.EXPENSE }?.id
                        ?: "other"
                    result.add(
                        TransactionEntity(
                            id = "${transactionId}_mirror",
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
                            paymentMethodId = null,
                            groupId = commonGroupId,
                        ),
                    )
                }
                TransactionType.EXPENSE -> {
                    if (selectedCard?.cardType == CreditCardType.REVOLVING) {
                        val incomeCategoryId = availableCategories.find { it.id == "credit_card_adjustment" }?.id
                            ?: availableCategories.firstOrNull { it.type == TransactionType.INCOME }?.id
                            ?: "salary"
                        result.add(
                            TransactionEntity(
                                id = "${transactionId}_mirror",
                                date = dateToSave,
                                description = "[${selectedCard?.name ?: "Credit Card"}] ${uiState.description.trim()}",
                                amount = amount,
                                categoryId = incomeCategoryId,
                                type = TransactionType.INCOME,
                                isCreditCard = false,
                                originalAmount = originalAmount,
                                originalCurrency = uiState.originalCurrency,
                                effectiveDate = dateToSave,
                                creditCardId = selectedCard?.id ?: uiState.creditCardId,
                                paymentMethodId = selectedCard?.id ?: uiState.selectedPaymentMethodId ?: uiState.creditCardId,
                                groupId = commonGroupId,
                            ),
                        )
                    }
                }
            }
        }

        result.add(
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
                paymentMethodId = uiState.selectedPaymentMethodId ?: uiState.creditCardId,
                recurrenceType = transactionToEdit?.recurrenceType ?: RecurrenceType.NONE,
                recurrenceLimit = transactionToEdit?.recurrenceLimit,
            ),
        )
        return result
    }
}
