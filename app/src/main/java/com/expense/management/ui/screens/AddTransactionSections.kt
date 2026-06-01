package com.expense.management.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.RecurrenceType
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.PaymentProvider
import com.expense.management.utils.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TypeSelector(
    type: TransactionType,
    onTypeChange: (TransactionType) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        val expenseSelected = type == TransactionType.EXPENSE
        SegmentedButton(
            selected = expenseSelected,
            onClick = { onTypeChange(TransactionType.EXPENSE) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.error,
                activeContentColor = MaterialTheme.colorScheme.onError,
                inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Text(stringResource(R.string.expense_type), fontWeight = FontWeight.Bold)
        }

        val incomeSelected = type == TransactionType.INCOME
        SegmentedButton(
            selected = incomeSelected,
            onClick = { onTypeChange(TransactionType.INCOME) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.primary,
                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Text(stringResource(R.string.income_type), fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun BasicDetailsCard(
    uiState: AddTransactionUiState,
    onEvent: (AddTransactionEvent) -> Unit,
    transactionToEdit: TransactionEntity?,
    currencySymbol: String,
    dateFormat: String,
    suggestions: List<String>,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
    onLaunchCamera: () -> Unit = {},
    onLaunchGallery: () -> Unit = {},
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (sharedTransitionScope != null && animatedVisibilityScope != null && transactionToEdit != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedElement(
                            rememberSharedContentState(key = "transaction_${transactionToEdit.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        BasicDetailsFields(
            uiState = uiState,
            onEvent = onEvent,
            currencySymbol = currencySymbol,
            dateFormat = dateFormat,
            suggestions = suggestions,
            onLaunchCamera = onLaunchCamera,
            onLaunchGallery = onLaunchGallery,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicDetailsFields(
    uiState: AddTransactionUiState,
    onEvent: (AddTransactionEvent) -> Unit,
    currencySymbol: String,
    dateFormat: String,
    suggestions: List<String>,
    onLaunchCamera: () -> Unit = {},
    onLaunchGallery: () -> Unit = {},
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.basic_details_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        OutlinedTextField(
            value = uiState.dateStr,
            onValueChange = { onEvent(AddTransactionEvent.OnDateChange(it)) },
            label = { Text(stringResource(R.string.transaction_date)) },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { onEvent(AddTransactionEvent.OnShowDatePicker(true)) }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.select_date_desc))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = uiState.isDescriptionExpanded && suggestions.isNotEmpty(),
            onExpandedChange = { onEvent(AddTransactionEvent.OnDescriptionExpandedChange(it)) },
        ) {
            OutlinedTextField(
                value = uiState.description,
                onValueChange = {
                    onEvent(AddTransactionEvent.OnDescriptionChange(it))
                    onEvent(AddTransactionEvent.OnDescriptionExpandedChange(true))
                },
                label = { Text(stringResource(R.string.description)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (uiState.description.isNotEmpty()) {
                        IconButton(onClick = {
                            onEvent(AddTransactionEvent.OnDescriptionChange(""))
                            onEvent(AddTransactionEvent.OnDescriptionExpandedChange(false))
                        }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                        }
                    }
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            )
            ExposedDropdownMenu(
                expanded = uiState.isDescriptionExpanded && suggestions.isNotEmpty(),
                onDismissRequest = { onEvent(AddTransactionEvent.OnDescriptionExpandedChange(false)) },
            ) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(text = suggestion) },
                        onClick = {
                            onEvent(AddTransactionEvent.OnDescriptionChange(suggestion))
                            onEvent(AddTransactionEvent.OnDescriptionExpandedChange(false))
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.originalCurrency == currencySymbol) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                androidx.compose.material3.TextButton(
                    onClick = { onEvent(AddTransactionEvent.OnShowCurrencyDialog(true)) },
                ) {
                    Text(stringResource(R.string.set_original_currency))
                }
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        AnimatedVisibility(visible = uiState.originalCurrency != currencySymbol) {
            Card(
                modifier = Modifier.padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = uiState.originalAmountText,
                            onValueChange = { onEvent(AddTransactionEvent.OnOriginalAmountChange(it.replace(',', '.'))) },
                            label = { Text(stringResource(R.string.amount_original_label)) },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = uiState.originalCurrency,
                            onValueChange = { onEvent(AddTransactionEvent.OnOriginalCurrencyChange(it.uppercase(Locale.ROOT))) },
                            label = { Text(stringResource(R.string.currency_original_label)) },
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Default.Check, contentDescription = stringResource(R.string.currency_selected), tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier
                                .weight(0.7f)
                                .clickable { onEvent(AddTransactionEvent.OnShowCurrencyDialog(true)) },
                            shape = RoundedCornerShape(12.dp),
                        )
                    }

                    Button(
                        onClick = {
                            onEvent(AddTransactionEvent.OnConvertAmount(uiState.originalCurrency, currencySymbol, uiState.originalAmountText.replace(',', '.').toDoubleOrNull() ?: 0.0))
                        },
                        enabled = !uiState.isConverting && uiState.originalAmountText.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    ) {
                        if (uiState.isConverting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.converting))
                        } else {
                            Icon(Icons.Default.SwapHoriz, contentDescription = stringResource(R.string.convert_currency_desc))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.convert_currency_desc))
                        }
                    }

                    Text(
                        text = stringResource(R.string.main_currency_hint, currencySymbol),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        OutlinedTextField(
            value = uiState.amountText,
            onValueChange = { onEvent(AddTransactionEvent.OnAmountChange(it.replace(',', '.'))) },
            label = { Text(stringResource(R.string.amount_converted_label, currencySymbol)) },
            placeholder = { Text("0.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(
                onClick = onLaunchCamera,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = stringResource(R.string.scan_receipt_camera), modifier = Modifier.size(20.dp))
            }
            IconButton(
                onClick = onLaunchGallery,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = stringResource(R.string.scan_receipt_gallery), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun RecurrenceSection(
    uiState: AddTransactionUiState,
    onEvent: (AddTransactionEvent) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        RecurrenceFields(uiState = uiState, onEvent = onEvent)
    }
}

@Composable
fun RecurrenceFields(
    uiState: AddTransactionUiState,
    onEvent: (AddTransactionEvent) -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.recurrence_label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Switch(
                checked = uiState.isRecurrenceEnabled,
                onCheckedChange = { onEvent(AddTransactionEvent.OnRecurrenceEnabledChange(it)) },
            )
        }

        AnimatedVisibility(visible = uiState.isRecurrenceEnabled) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                OutlinedTextField(
                    value = when (uiState.recurrenceType) {
                        RecurrenceType.DAILY -> stringResource(R.string.recurrence_daily)
                        RecurrenceType.WEEKLY -> stringResource(R.string.recurrence_weekly)
                        RecurrenceType.MONTHLY -> stringResource(R.string.recurrence_monthly)
                        RecurrenceType.YEARLY -> stringResource(R.string.recurrence_yearly)
                        else -> ""
                    },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { onEvent(AddTransactionEvent.OnShowRecurrenceTypeDialog(true)) }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.select_recurrence))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { onEvent(AddTransactionEvent.OnShowRecurrenceTypeDialog(true)) },
                    shape = RoundedCornerShape(12.dp),
                )

                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = stringResource(R.string.recurrence_occurrences, uiState.recurrenceLimit),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Slider(
                        value = uiState.recurrenceLimit.toFloat(),
                        onValueChange = { onEvent(AddTransactionEvent.OnRecurrenceLimitChange(it.toInt())) },
                        valueRange = 1f..60f,
                        steps = 59,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.recurrence_occurrences_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodSection(
    uiState: AddTransactionUiState,
    onEvent: (AddTransactionEvent) -> Unit,
    isEditing: Boolean,
    transactionToEdit: TransactionEntity?,
    activeCreditCards: List<ActiveCreditCard>,
    allPaymentMethods: List<PaymentMethodEntity> = emptyList(),
    isCC: Boolean = false,
) {
    val nonCardMethods = allPaymentMethods.filter {
        it.provider != PaymentProvider.CREDIT_CARD_SALDO.name &&
            it.provider != PaymentProvider.CREDIT_CARD_REVOLVING.name
    }
    val showNonCardSection = nonCardMethods.isNotEmpty()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.payment_method_label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Generic payment method selector (all providers + legacy fallback)
            if (allPaymentMethods.isNotEmpty() || activeCreditCards.isNotEmpty()) {
                val selectedName = when {
                    uiState.selectedPaymentMethodId != null ->
                        allPaymentMethods.find { it.id == uiState.selectedPaymentMethodId }?.name
                            ?: activeCreditCards.find { it.id == uiState.selectedPaymentMethodId }?.name
                    uiState.creditCardId != null ->
                        activeCreditCards.find { it.id == uiState.creditCardId }?.name
                            ?: allPaymentMethods.find { it.id == uiState.creditCardId }?.name
                    else -> null
                }
                OutlinedTextField(
                    value = selectedName ?: stringResource(R.string.select_credit_card),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.payment_method_label)) },
                    trailingIcon = {
                        IconButton(onClick = { onEvent(AddTransactionEvent.OnShowCreditCardDialog(true)) }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.select_payment_method))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { onEvent(AddTransactionEvent.OnShowCreditCardDialog(true)) },
                )
            }

            // Credit card toggle checkbox (only visible when a credit card is selected)
            val selectedMethodId = uiState.selectedPaymentMethodId ?: uiState.creditCardId
            val isCreditCardSelected = selectedMethodId != null &&
                allPaymentMethods.find { it.id == selectedMethodId }?.let {
                    it.provider == PaymentProvider.CREDIT_CARD_SALDO.name ||
                        it.provider == PaymentProvider.CREDIT_CARD_REVOLVING.name
                } ?: uiState.isCreditCard

            AnimatedVisibility(visible = isCreditCardSelected || uiState.isCreditCard) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (!isEditing) {
                                Modifier.clickable { onEvent(AddTransactionEvent.OnCreditCardToggle(!uiState.isCreditCard)) }
                            } else {
                                Modifier
                            },
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = uiState.isCreditCard,
                        onCheckedChange = { if (!isEditing) onEvent(AddTransactionEvent.OnCreditCardToggle(it)) },
                        enabled = !isEditing,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.use_credit_card_label), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(
                            text = stringResource(R.string.credit_card_payment_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            val showInstallmentCheckbox = !isEditing && !uiState.isCreditCard || (isEditing && transactionToEdit?.totalInstallments != null && transactionToEdit.totalInstallments > 1 && !transactionToEdit.isCreditCard)
            AnimatedVisibility(visible = showInstallmentCheckbox) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (!isEditing) {
                                    Modifier.clickable { onEvent(AddTransactionEvent.OnIsInstallmentChange(!uiState.isInstallment)) }
                                } else {
                                    Modifier
                                },
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = uiState.isInstallment,
                            onCheckedChange = { onEvent(AddTransactionEvent.OnIsInstallmentChange(it)) },
                            enabled = !isEditing,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.installment_payment), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                text = stringResource(R.string.installment_payment_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InstallmentSection(
    uiState: AddTransactionUiState,
    onEvent: (AddTransactionEvent) -> Unit,
    isEditing: Boolean,
    transactionToEdit: TransactionEntity?,
    currencySymbol: String,
    dateFormat: String,
    activeCreditCards: List<ActiveCreditCard>,
) {
    val showSection = (uiState.isCreditCard || uiState.isInstallment) && (uiState.type == TransactionType.EXPENSE || uiState.type == TransactionType.INCOME)
    AnimatedVisibility(visible = showSection) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedCornerShape(16.dp),
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(16.dp),
                )
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.payment_options),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            if (uiState.isInstallment) {
                if (!isEditing) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                    ) {
                        SegmentedButton(
                            selected = uiState.calculationMode == "installments",
                            onClick = { onEvent(AddTransactionEvent.OnCalculationModeChange("installments")) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) {
                            Text(stringResource(R.string.calc_mode_installments))
                        }
                        SegmentedButton(
                            selected = uiState.calculationMode == "amount",
                            onClick = { onEvent(AddTransactionEvent.OnCalculationModeChange("amount")) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) {
                            Text(stringResource(R.string.calc_mode_amount))
                        }
                    }
                }

                if (uiState.calculationMode == "installments" || isEditing) {
                    Text(stringResource(R.string.number_of_installments, uiState.installmentsCount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = uiState.installmentsCount.toFloat(),
                        onValueChange = { onEvent(AddTransactionEvent.OnInstallmentsCountChange(it.toInt())) },
                        valueRange = 2f..12f,
                        steps = 10,
                        enabled = !isEditing,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    val amount = uiState.amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    if (amount > 0 && uiState.installmentsCount > 0) {
                        val amountPerInstallment = amount / uiState.installmentsCount
                        Text(
                            text = stringResource(R.string.calc_amount_per_installment, String.format(Locale.US, "%.2f %s", amountPerInstallment, currencySymbol)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = uiState.installmentAmountText,
                        onValueChange = { onEvent(AddTransactionEvent.OnInstallmentAmountChange(it.replace(',', '.'))) },
                        label = { Text(stringResource(R.string.installment_amount_label)) },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )

                    val totalAmount = uiState.amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val installmentAmount = uiState.installmentAmountText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    if (totalAmount > 0 && installmentAmount > 0) {
                        val calculatedInstallments = kotlin.math.ceil(totalAmount / installmentAmount).toInt()
                        Text(
                            text = stringResource(R.string.number_of_installments, calculatedInstallments),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isEditing || (isEditing && uiState.isCreditCard)) {
                    AnimatedVisibility(visible = uiState.isCreditCard) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEvent(AddTransactionEvent.OnApplyCcDelayChange(!uiState.applyCcDelayToInstallments)) }
                                    .padding(vertical = 8.dp),
                            ) {
                                Checkbox(
                                    checked = uiState.applyCcDelayToInstallments,
                                    onCheckedChange = { onEvent(AddTransactionEvent.OnApplyCcDelayChange(it)) },
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.apply_cc_delay),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = if (uiState.applyCcDelayToInstallments) {
                                            stringResource(R.string.cc_delay_installment_message_on)
                                        } else {
                                            stringResource(R.string.cc_delay_installment_message_off)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                if (!isEditing || (isEditing && !uiState.isCreditCard && uiState.isInstallment)) {
                    OutlinedTextField(
                        value = uiState.installmentStartDateStr,
                        onValueChange = { onEvent(AddTransactionEvent.OnInstallmentStartDateChange(it)) },
                        label = { Text(stringResource(R.string.first_installment_date)) },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { onEvent(AddTransactionEvent.OnShowInstallmentDatePicker(true)) }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.select_date_desc))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Text(
                        stringResource(R.string.first_installment_date_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            } else if (uiState.isCreditCard && !uiState.isInstallment) {
                val selectedCard = remember(uiState.creditCardId, activeCreditCards) {
                    activeCreditCards.find { it.id == uiState.creditCardId }
                }
                val displayFormatter = remember(dateFormat) { DateTimeFormatter.ofPattern(dateFormat) }
                val effectiveDate = try {
                    if (transactionToEdit != null && transactionToEdit.effectiveDate.isNotEmpty()) {
                        transactionToEdit.effectiveDate
                    } else {
                        val tDate = LocalDate.parse(uiState.dateStr, displayFormatter)
                        if (selectedCard != null) {
                            DateUtils.calculateEffectiveDate(
                                tDate,
                                DateUtils.CardDateInfo(selectedCard.closingDay, selectedCard.paymentDay),
                            )
                        } else {
                            tDate.plusMonths(1).withDayOfMonth(15).format(DateTimeFormatter.ISO_LOCAL_DATE)
                        }
                    }
                } catch (e: Exception) {
                    ""
                }

                val formattedEffectiveDate = try {
                    LocalDate.parse(effectiveDate, DateTimeFormatter.ISO_LOCAL_DATE).format(displayFormatter)
                } catch (e: Exception) {
                    effectiveDate
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.effective_date), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.expected_debit_date), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(formattedEffectiveDate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(
                    stringResource(R.string.expected_debit_date_calc, if (selectedCard != null) selectedCard.paymentDay else 15),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
fun TransactionBottomBar(
    isEditing: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.cancel))
                }
                if (isEditing && onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Button(
                onClick = onSave,
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) stringResource(R.string.update_transaction) else stringResource(R.string.save_transaction),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
