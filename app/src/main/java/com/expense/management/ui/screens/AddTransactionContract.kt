package com.expense.management.ui.screens

import com.expense.management.data.RecurrenceType
import com.expense.management.data.TransactionType
import com.expense.management.ui.model.DeleteType

data class AddTransactionUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val description: String = "",
    val isDescriptionExpanded: Boolean = false,
    val selectedCategory: String = "",
    val isCreditCard: Boolean = false,
    val creditCardId: String? = null,
    val showCreditCardDialog: Boolean = false,
    val selectedPaymentMethodId: String? = null,
    val originalAmountText: String = "",
    val originalCurrency: String = "",
    val showCurrencyDialog: Boolean = false,
    val isConverting: Boolean = false,
    val isInstallment: Boolean = false,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceLimit: Int = 12,
    val showRecurrenceTypeDialog: Boolean = false,
    val calculationMode: String = "installments",
    val installmentAmountText: String = "",
    val installmentsCount: Int = 3,
    val dateStr: String = "",
    val showDatePicker: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val installmentStartDateStr: String = "",
    val showInstallmentDatePicker: Boolean = false,
    val showPreviousMonthAlert: Boolean = false,
    val applyCcDelayToInstallments: Boolean = true,
    val ignoreDateWarning: Boolean = false,
)

sealed interface AddTransactionEvent {
    data class OnTypeChange(val type: TransactionType) : AddTransactionEvent
    data class OnAmountChange(val amount: String) : AddTransactionEvent
    data class OnDescriptionChange(val description: String) : AddTransactionEvent
    data class OnDescriptionExpandedChange(val expanded: Boolean) : AddTransactionEvent
    data class OnCategorySelected(val categoryId: String) : AddTransactionEvent
    data class OnCreditCardToggle(val isCreditCard: Boolean) : AddTransactionEvent
    data class OnCreditCardIdChange(val creditCardId: String?) : AddTransactionEvent
    data class OnShowCreditCardDialog(val show: Boolean) : AddTransactionEvent
    data class OnPaymentMethodSelected(val paymentMethodId: String?, val isCreditCard: Boolean) : AddTransactionEvent
    data class OnOriginalAmountChange(val amount: String) : AddTransactionEvent
    data class OnOriginalCurrencyChange(val currency: String) : AddTransactionEvent
    data class OnShowCurrencyDialog(val show: Boolean) : AddTransactionEvent
    data class OnIsInstallmentChange(val isInstallment: Boolean) : AddTransactionEvent
    data class OnRecurrenceTypeChange(val recurrenceType: RecurrenceType) : AddTransactionEvent
    data class OnRecurrenceLimitChange(val limit: Int) : AddTransactionEvent
    data class OnShowRecurrenceTypeDialog(val show: Boolean) : AddTransactionEvent
    data class OnCalculationModeChange(val mode: String) : AddTransactionEvent
    data class OnInstallmentAmountChange(val amount: String) : AddTransactionEvent
    data class OnInstallmentsCountChange(val count: Int) : AddTransactionEvent
    data class OnDateChange(val date: String) : AddTransactionEvent
    data class OnShowDatePicker(val show: Boolean) : AddTransactionEvent
    data class OnShowDeleteDialog(val show: Boolean) : AddTransactionEvent
    data class OnInstallmentStartDateChange(val date: String) : AddTransactionEvent
    data class OnShowInstallmentDatePicker(val show: Boolean) : AddTransactionEvent
    data class OnShowPreviousMonthAlert(val show: Boolean) : AddTransactionEvent
    data class OnApplyCcDelayChange(val apply: Boolean) : AddTransactionEvent
    data class OnIgnoreDateWarningChange(val ignore: Boolean) : AddTransactionEvent
    data object OnSave : AddTransactionEvent
    data class OnDelete(val transactionId: String, val deleteType: DeleteType) : AddTransactionEvent
    data class OnConvertAmount(val originalCurrency: String, val targetCurrency: String, val amount: Double) : AddTransactionEvent
}
