package com.expense.management.ui.screens

import com.expense.management.data.RecurrenceType
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.DeleteType

data class RegularTransactionUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val description: String = "",
    val isDescriptionExpanded: Boolean = false,
    val selectedCategory: String = "",
    val isPaymentMethodEnabled: Boolean = false,
    val selectedPaymentMethodId: String? = null,
    val originalAmountText: String = "",
    val originalCurrency: String = "",
    val showCurrencyDialog: Boolean = false,
    val isConverting: Boolean = false,
    val isRecurrenceEnabled: Boolean = false,
    val recurrenceType: RecurrenceType = RecurrenceType.MONTHLY,
    val recurrenceLimit: Int = 1,
    val showRecurrenceTypeDialog: Boolean = false,
    val dateStr: String = "",
    val showDatePicker: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showPreviousMonthAlert: Boolean = false,
    val ignoreDateWarning: Boolean = false,
)

sealed interface RegularTransactionEvent {
    data class OnTypeChange(val type: TransactionType) : RegularTransactionEvent
    data class OnAmountChange(val amount: String) : RegularTransactionEvent
    data class OnDescriptionChange(val description: String) : RegularTransactionEvent
    data class OnDescriptionExpandedChange(val expanded: Boolean) : RegularTransactionEvent
    data class OnCategorySelected(val categoryId: String) : RegularTransactionEvent
    data class OnPaymentMethodEnabledChange(val enabled: Boolean) : RegularTransactionEvent
    data class OnPaymentMethodSelected(val paymentMethodId: String?) : RegularTransactionEvent
    data class OnOriginalAmountChange(val amount: String) : RegularTransactionEvent
    data class OnOriginalCurrencyChange(val currency: String) : RegularTransactionEvent
    data class OnShowCurrencyDialog(val show: Boolean) : RegularTransactionEvent
    data class OnRecurrenceEnabledChange(val enabled: Boolean) : RegularTransactionEvent
    data class OnRecurrenceTypeChange(val recurrenceType: RecurrenceType) : RegularTransactionEvent
    data class OnRecurrenceLimitChange(val limit: Int) : RegularTransactionEvent
    data class OnShowRecurrenceTypeDialog(val show: Boolean) : RegularTransactionEvent
    data class OnDateChange(val date: String) : RegularTransactionEvent
    data class OnShowDatePicker(val show: Boolean) : RegularTransactionEvent
    data class OnShowDeleteDialog(val show: Boolean) : RegularTransactionEvent
    data class OnShowPreviousMonthAlert(val show: Boolean) : RegularTransactionEvent
    data class OnIgnoreDateWarningChange(val ignore: Boolean) : RegularTransactionEvent
    data object OnSave : RegularTransactionEvent
    data class OnDelete(val transactionId: String, val deleteType: DeleteType) : RegularTransactionEvent
    data class OnConvertAmount(val originalCurrency: String, val targetCurrency: String, val amount: Double) : RegularTransactionEvent
}
