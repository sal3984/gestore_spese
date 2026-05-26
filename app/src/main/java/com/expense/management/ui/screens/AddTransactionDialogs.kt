package com.expense.management.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.data.CategoryEntity
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.RecurrenceType
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.PaymentProvider
import com.expense.management.ui.model.DeleteType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun CurrencyDialog(
    currentCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    additionalCurrencies: List<String> = listOf("USD", "EUR", "GBP", "JPY", "CHF", "HUF"),
) {
    val allCurrencies = remember(currentCurrency, additionalCurrencies) {
        (listOf(currentCurrency) + additionalCurrencies).distinct()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.original_currency_dialog_title)) },
        text = {
            Column {
                allCurrencies.forEach { symbol ->
                    Text(
                        text = symbol,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCurrencySelected(symbol) }
                            .heightIn(min = 48.dp)
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun CreditCardDialog(
    activeCreditCards: List<com.expense.management.domain.model.ActiveCreditCard>,
    currentCardId: String?,
    onCardSelected: (cardId: String, isRevolving: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_credit_card)) },
        text = {
            Column {
                activeCreditCards.forEach { card ->
                    Text(
                        text = card.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCardSelected(card.id, card.cardType == com.expense.management.domain.model.CreditCardType.REVOLVING)
                            }
                            .heightIn(min = 48.dp)
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun PaymentMethodPickerDialog(
    allPaymentMethods: List<PaymentMethodEntity>,
    currentMethodId: String?,
    onMethodSelected: (methodId: String, isCreditCard: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.payment_method_label)) },
        text = {
            Column {
                allPaymentMethods.forEach { method ->
                    val isCreditCard = method.provider == PaymentProvider.CREDIT_CARD_SALDO.name ||
                        method.provider == PaymentProvider.CREDIT_CARD_REVOLVING.name
                    Text(
                        text = method.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (method.id == currentMethodId) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMethodSelected(method.id, isCreditCard) }
                            .heightIn(min = 48.dp)
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun RecurrenceTypeDialog(
    currentRecurrence: RecurrenceType,
    onRecurrenceSelected: (RecurrenceType) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recurrence_label)) },
        text = {
            Column {
                RecurrenceType.entries.filter { it != RecurrenceType.NONE }.forEach { typeEntry ->
                    Text(
                        text = when (typeEntry) {
                            RecurrenceType.DAILY -> stringResource(R.string.recurrence_daily)
                            RecurrenceType.WEEKLY -> stringResource(R.string.recurrence_weekly)
                            RecurrenceType.MONTHLY -> stringResource(R.string.recurrence_monthly)
                            RecurrenceType.YEARLY -> stringResource(R.string.recurrence_yearly)
                            else -> ""
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (typeEntry == currentRecurrence) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onRecurrenceSelected(typeEntry)
                                onDismiss()
                            }
                            .heightIn(min = 48.dp)
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun PreviousMonthDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.warning_past_date_title)) },
        text = { Text(stringResource(R.string.warning_past_date_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.proceed_and_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel).uppercase())
            }
        },
    )
}

@Composable
fun TransactionDatePicker(
    currentDate: String,
    dateFormat: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val displayFormatter = remember(dateFormat) { DateTimeFormatter.ofPattern(dateFormat) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            LocalDate.parse(currentDate, displayFormatter)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            Instant.now().toEpochMilli()
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    onDateSelected(selectedDate.format(displayFormatter))
                }
                onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun InstallmentDatePicker(
    currentDate: String,
    dateFormat: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val displayFormatter = remember(dateFormat) { DateTimeFormatter.ofPattern(dateFormat) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            LocalDate.parse(currentDate, displayFormatter)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            Instant.now().toEpochMilli()
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    onDateSelected(selectedDate.format(displayFormatter))
                }
                onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun DeleteDialog(
    transactionToEdit: TransactionEntity?,
    onDelete: (String, DeleteType) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_transaction_title)) },
        text = {
            Text(
                if (transactionToEdit?.groupId != null && (transactionToEdit.totalInstallments ?: 0) > 1) {
                    stringResource(R.string.delete_installment_message)
                } else if (transactionToEdit?.groupId != null && transactionToEdit.recurrenceType != RecurrenceType.NONE) {
                    stringResource(R.string.delete_recurrence_message)
                } else {
                    stringResource(R.string.delete_transaction_message)
                },
            )
        },
        confirmButton = {
            if (transactionToEdit?.groupId != null && ((transactionToEdit.totalInstallments ?: 0) > 1 || transactionToEdit.recurrenceType != RecurrenceType.NONE)) {
                Column {
                    TextButton(
                        onClick = {
                            onDelete(transactionToEdit.id, DeleteType.THIS_AND_SUBSEQUENT)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(R.string.delete_this_and_subsequent))
                    }
                    TextButton(
                        onClick = {
                            onDelete(transactionToEdit.id, DeleteType.SINGLE)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(R.string.delete_single_installment))
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel).uppercase())
                    }
                }
            } else {
                TextButton(
                    onClick = {
                        transactionToEdit?.let { onDelete(it.id, DeleteType.SINGLE) }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.delete_uppercase))
                }
            }
        },
        dismissButton = {
            if (transactionToEdit?.groupId == null || ((transactionToEdit.totalInstallments ?: 0) <= 1 && transactionToEdit.recurrenceType == RecurrenceType.NONE)) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

@Composable
fun CategoryPickerDialog(
    type: TransactionType,
    availableCategories: List<CategoryEntity>,
    selectedCategoryId: String?,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val categories = remember(availableCategories, type) {
        availableCategories.filter { it.type == type }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.all_categories)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                val chunks = remember(categories) { categories.chunked(3) }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    chunks.forEach { rowCategories ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowCategories.forEach { category ->
                                key(category.id) {
                                    CategoryGridItem(
                                        modifier = Modifier.weight(1f),
                                        category = category,
                                        isSelected = selectedCategoryId == category.id,
                                        onClick = {
                                            onCategorySelected(category.id)
                                            onDismiss()
                                        },
                                    )
                                }
                            }
                            if (rowCategories.size < 3) {
                                repeat(3 - rowCategories.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
