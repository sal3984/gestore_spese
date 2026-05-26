package com.expense.management.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.expense.management.domain.usecase.RegularTransactionSaveResult
import com.expense.management.domain.usecase.RegularTransactionSaveUseCase
import com.expense.management.ui.model.DeleteType
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import androidx.compose.ui.text.intl.Locale as ComposeLocale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AddRegularTransactionScreen(
    modifier: Modifier = Modifier,
    transactionToEdit: TransactionEntity?,
    currencySymbol: String,
    dateFormat: String,
    suggestions: List<String>,
    availableCategories: List<CategoryEntity>,
    onSave: (TransactionEntity) -> Unit,
    onDelete: (String, DeleteType) -> Unit,
    onBack: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onConvertAmount: suspend (String, String, Double) -> Double? = { _, _, _ -> null },
    allPaymentMethods: List<PaymentMethodEntity> = emptyList(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
    frequentExpenseCategories: List<CategoryEntity> = emptyList(),
    frequentIncomeCategories: List<CategoryEntity> = emptyList(),
) {
    val displayFormatter = remember(dateFormat) { DateTimeFormatter.ofPattern(dateFormat) }
    val locale = ComposeLocale.current.platformLocale
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isEditing = transactionToEdit != null

    val nonCreditCardMethods = remember(allPaymentMethods) {
        allPaymentMethods.filter { method ->
            val provider = try {
                PaymentProvider.valueOf(method.provider)
            } catch (_: Exception) {
                null
            }
            provider != PaymentProvider.CREDIT_CARD_SALDO && provider != PaymentProvider.CREDIT_CARD_REVOLVING
        }
    }

    var uiState by remember(transactionToEdit) {
        mutableStateOf(
            RegularTransactionUiState(
                type = transactionToEdit?.type ?: TransactionType.EXPENSE,
                amountText = transactionToEdit?.amount?.toString() ?: "",
                description = transactionToEdit?.description ?: "",
                selectedCategory = transactionToEdit?.categoryId.takeIf { id -> availableCategories.any { it.id == id } }
                    ?: transactionToEdit?.categoryId.takeIf { transactionToEdit != null }
                    ?: availableCategories.firstOrNull { it.type == TransactionType.EXPENSE }?.id
                    ?: "food",
                selectedPaymentMethodId = transactionToEdit?.paymentMethodId,
                originalAmountText = transactionToEdit?.originalAmount?.toString() ?: "",
                originalCurrency = transactionToEdit?.originalCurrency ?: currencySymbol,
                isRecurrenceEnabled = transactionToEdit?.recurrenceType?.let { it != RecurrenceType.NONE } ?: false,
                recurrenceType = transactionToEdit?.recurrenceType ?: RecurrenceType.MONTHLY,
                recurrenceLimit = transactionToEdit?.recurrenceLimit ?: 1,
                dateStr = if (transactionToEdit != null) {
                    try {
                        LocalDate.parse(transactionToEdit.date, DateTimeFormatter.ISO_LOCAL_DATE).format(displayFormatter)
                    } catch (_: DateTimeParseException) {
                        transactionToEdit.date
                    }
                } else {
                    LocalDate.now().format(displayFormatter)
                },
                isPaymentMethodEnabled = transactionToEdit?.paymentMethodId != null,
            ),
        )
    }

    val errorInvalidInput = stringResource(R.string.error_invalid_input)
    val errorInvalidDateFormat = stringResource(R.string.error_invalid_date_format)
    val errorPastLimitDate = stringResource(R.string.error_past_limit_date)
    val errorConversionFailed = stringResource(R.string.error_conversion_failed)
    val okLabel = stringResource(R.string.ok)

    val saveUseCase = remember { RegularTransactionSaveUseCase() }

    fun trySave() {
        when (val result = saveUseCase(uiState, transactionToEdit, availableCategories, dateFormat, locale)) {
            is RegularTransactionSaveResult.Ready -> {
                result.transactions.forEach { onSave(it) }
                onBack()
            }
            is RegularTransactionSaveResult.PreviousMonthWarning -> {
                uiState = uiState.copy(showPreviousMonthAlert = true)
            }
            is RegularTransactionSaveResult.Error -> {
                val message = when {
                    result.message == "error_invalid_input" -> errorInvalidInput
                    result.message == "error_invalid_date_format" -> errorInvalidDateFormat
                    result.message.startsWith("error_past_limit_date:") -> {
                        String.format(errorPastLimitDate, result.message.removePrefix("error_past_limit_date:"))
                    }
                    else -> result.message
                }
                scope.launch { snackbarHostState.showSnackbar(message, okLabel) }
            }
        }
    }

    fun handleEvent(event: RegularTransactionEvent) {
        when (event) {
            is RegularTransactionEvent.OnTypeChange -> {
                val newCategory = availableCategories.firstOrNull { it.type == event.type }
                uiState = uiState.copy(type = event.type, selectedCategory = newCategory?.id ?: uiState.selectedCategory)
            }
            is RegularTransactionEvent.OnAmountChange -> uiState = uiState.copy(amountText = event.amount)
            is RegularTransactionEvent.OnDescriptionChange -> {
                uiState = uiState.copy(description = event.description)
                onDescriptionChange(event.description)
            }
            is RegularTransactionEvent.OnDescriptionExpandedChange -> uiState = uiState.copy(isDescriptionExpanded = event.expanded)
            is RegularTransactionEvent.OnCategorySelected -> uiState = uiState.copy(selectedCategory = event.categoryId)
            is RegularTransactionEvent.OnPaymentMethodEnabledChange -> uiState = uiState.copy(isPaymentMethodEnabled = event.enabled)
            is RegularTransactionEvent.OnPaymentMethodSelected -> uiState = uiState.copy(selectedPaymentMethodId = event.paymentMethodId)
            is RegularTransactionEvent.OnOriginalAmountChange -> uiState = uiState.copy(originalAmountText = event.amount)
            is RegularTransactionEvent.OnOriginalCurrencyChange -> uiState = uiState.copy(originalCurrency = event.currency)
            is RegularTransactionEvent.OnShowCurrencyDialog -> uiState = uiState.copy(showCurrencyDialog = event.show)
            is RegularTransactionEvent.OnRecurrenceEnabledChange -> uiState = uiState.copy(
                isRecurrenceEnabled = event.enabled,
                recurrenceType = if (event.enabled) uiState.recurrenceType else RecurrenceType.NONE,
            )
            is RegularTransactionEvent.OnRecurrenceTypeChange -> uiState = uiState.copy(recurrenceType = event.recurrenceType)
            is RegularTransactionEvent.OnRecurrenceLimitChange -> uiState = uiState.copy(recurrenceLimit = event.limit)
            is RegularTransactionEvent.OnShowRecurrenceTypeDialog -> uiState = uiState.copy(showRecurrenceTypeDialog = event.show)
            is RegularTransactionEvent.OnDateChange -> uiState = uiState.copy(dateStr = event.date)
            is RegularTransactionEvent.OnShowDatePicker -> uiState = uiState.copy(showDatePicker = event.show)
            is RegularTransactionEvent.OnShowDeleteDialog -> uiState = uiState.copy(showDeleteDialog = event.show)
            is RegularTransactionEvent.OnShowPreviousMonthAlert -> uiState = uiState.copy(showPreviousMonthAlert = event.show)
            is RegularTransactionEvent.OnIgnoreDateWarningChange -> uiState = uiState.copy(ignoreDateWarning = event.ignore)
            RegularTransactionEvent.OnSave -> trySave()
            is RegularTransactionEvent.OnDelete -> {
                onDelete(event.transactionId, event.deleteType)
                uiState = uiState.copy(showDeleteDialog = false)
            }
            is RegularTransactionEvent.OnConvertAmount -> {
                if (event.amount > 0) {
                    scope.launch {
                        uiState = uiState.copy(isConverting = true)
                        val result = onConvertAmount(event.originalCurrency, event.targetCurrency, event.amount)
                        uiState = uiState.copy(isConverting = false)
                        if (result != null) {
                            uiState = uiState.copy(amountText = String.format(Locale.US, "%.2f", result))
                        } else {
                            snackbarHostState.showSnackbar(errorConversionFailed)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (transactionToEdit == null) stringResource(R.string.add_transaction) else stringResource(R.string.edit_transaction), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (transactionToEdit != null) {
                        IconButton(onClick = { handleEvent(RegularTransactionEvent.OnShowDeleteDialog(true)) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            SaveButton(
                isEditing = isEditing,
                onSave = { handleEvent(RegularTransactionEvent.OnSave) },
            )
        },
    ) { padding ->
        RegularTransactionContent(
            modifier = modifier,
            uiState = uiState,
            onEvent = ::handleEvent,
            isEditing = isEditing,
            transactionToEdit = transactionToEdit,
            currencySymbol = currencySymbol,
            dateFormat = dateFormat,
            suggestions = suggestions,
            availableCategories = availableCategories,
            nonCreditCardMethods = nonCreditCardMethods,
            frequentExpenseCategories = frequentExpenseCategories,
            frequentIncomeCategories = frequentIncomeCategories,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            padding = padding,
        )
    }

    if (uiState.showCurrencyDialog) {
        CurrencyDialog(
            currentCurrency = uiState.originalCurrency,
            onCurrencySelected = { handleEvent(RegularTransactionEvent.OnOriginalCurrencyChange(it)) },
            onDismiss = { handleEvent(RegularTransactionEvent.OnShowCurrencyDialog(false)) },
        )
    }

    if (uiState.showRecurrenceTypeDialog) {
        RecurrenceTypeDialog(
            currentRecurrence = uiState.recurrenceType,
            onRecurrenceSelected = { handleEvent(RegularTransactionEvent.OnRecurrenceTypeChange(it)) },
            onDismiss = { handleEvent(RegularTransactionEvent.OnShowRecurrenceTypeDialog(false)) },
        )
    }

    if (uiState.showPreviousMonthAlert) {
        PreviousMonthDialog(
            onConfirm = {
                handleEvent(RegularTransactionEvent.OnIgnoreDateWarningChange(true))
                trySave()
            },
            onDismiss = { handleEvent(RegularTransactionEvent.OnShowPreviousMonthAlert(false)) },
        )
    }

    if (uiState.showDatePicker) {
        TransactionDatePicker(
            currentDate = uiState.dateStr,
            dateFormat = dateFormat,
            onDateSelected = { handleEvent(RegularTransactionEvent.OnDateChange(it)) },
            onDismiss = { handleEvent(RegularTransactionEvent.OnShowDatePicker(false)) },
        )
    }

    if (uiState.showDeleteDialog) {
        DeleteDialog(
            transactionToEdit = transactionToEdit,
            onDelete = { id, type -> handleEvent(RegularTransactionEvent.OnDelete(id, type)) },
            onDismiss = { handleEvent(RegularTransactionEvent.OnShowDeleteDialog(false)) },
        )
    }
}

@Composable
private fun RegularTransactionContent(
    modifier: Modifier,
    uiState: RegularTransactionUiState,
    onEvent: (RegularTransactionEvent) -> Unit,
    isEditing: Boolean,
    transactionToEdit: TransactionEntity?,
    currencySymbol: String,
    dateFormat: String,
    suggestions: List<String>,
    availableCategories: List<CategoryEntity>,
    nonCreditCardMethods: List<PaymentMethodEntity>,
    frequentExpenseCategories: List<CategoryEntity>,
    frequentIncomeCategories: List<CategoryEntity>,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope?,
    padding: PaddingValues,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TypeSelector(
            type = uiState.type,
            onTypeChange = { onEvent(RegularTransactionEvent.OnTypeChange(it)) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        RegularBasicDetailsCard(
            transactionToEdit = transactionToEdit,
            uiState = uiState,
            onEvent = onEvent,
            currencySymbol = currencySymbol,
            dateFormat = dateFormat,
            suggestions = suggestions,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )

        if (!isEditing) {
            Spacer(modifier = Modifier.height(12.dp))
            RegularRecurrenceSection(
                uiState = uiState,
                onEvent = onEvent,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        RegularPaymentMethodSection(
            isPaymentMethodEnabled = uiState.isPaymentMethodEnabled,
            selectedPaymentMethodId = uiState.selectedPaymentMethodId,
            allPaymentMethods = nonCreditCardMethods,
            onEnabledChange = { onEvent(RegularTransactionEvent.OnPaymentMethodEnabledChange(it)) },
            onMethodSelected = { onEvent(RegularTransactionEvent.OnPaymentMethodSelected(it)) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            stringResource(R.string.category_selection_label),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )

        CategorySelector(
            type = uiState.type,
            selectedCategoryId = uiState.selectedCategory,
            onCategorySelected = { onEvent(RegularTransactionEvent.OnCategorySelected(it)) },
            availableCategories = availableCategories,
            frequentExpenseCategories = frequentExpenseCategories,
            frequentIncomeCategories = frequentIncomeCategories,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegularBasicDetailsCard(
    transactionToEdit: TransactionEntity?,
    uiState: RegularTransactionUiState,
    onEvent: (RegularTransactionEvent) -> Unit,
    currencySymbol: String,
    dateFormat: String,
    suggestions: List<String>,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope?,
) {
    Card(
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.basic_details_label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            OutlinedTextField(
                value = uiState.amountText,
                onValueChange = { onEvent(RegularTransactionEvent.OnAmountChange(it.replace(',', '.'))) },
                label = { Text(stringResource(R.string.amount_converted_label, currencySymbol)) },
                placeholder = { Text("0.00") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.originalCurrency == currencySymbol) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = { onEvent(RegularTransactionEvent.OnShowCurrencyDialog(true)) }) {
                        Text(stringResource(R.string.set_original_currency))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.dateStr,
                onValueChange = { onEvent(RegularTransactionEvent.OnDateChange(it)) },
                label = { Text(stringResource(R.string.transaction_date)) },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { onEvent(RegularTransactionEvent.OnShowDatePicker(true)) }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.select_date_desc))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            androidx.compose.material3.ExposedDropdownMenuBox(
                expanded = uiState.isDescriptionExpanded && suggestions.isNotEmpty(),
                onExpandedChange = { onEvent(RegularTransactionEvent.OnDescriptionExpandedChange(it)) },
            ) {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = {
                        onEvent(RegularTransactionEvent.OnDescriptionChange(it))
                        onEvent(RegularTransactionEvent.OnDescriptionExpandedChange(true))
                    },
                    label = { Text(stringResource(R.string.description)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (uiState.description.isNotEmpty()) {
                            IconButton(onClick = {
                                onEvent(RegularTransactionEvent.OnDescriptionChange(""))
                                onEvent(RegularTransactionEvent.OnDescriptionExpandedChange(false))
                            }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                            }
                        }
                    },
                )
                androidx.compose.material3.DropdownMenu(
                    expanded = uiState.isDescriptionExpanded && suggestions.isNotEmpty(),
                    onDismissRequest = { onEvent(RegularTransactionEvent.OnDescriptionExpandedChange(false)) },
                ) {
                    suggestions.forEach { suggestion ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                onEvent(RegularTransactionEvent.OnDescriptionChange(suggestion))
                                onEvent(RegularTransactionEvent.OnDescriptionExpandedChange(false))
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegularRecurrenceSection(
    uiState: RegularTransactionUiState,
    onEvent: (RegularTransactionEvent) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.recurrence_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Switch(
                    checked = uiState.isRecurrenceEnabled,
                    onCheckedChange = { onEvent(RegularTransactionEvent.OnRecurrenceEnabledChange(it)) },
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
                            else -> stringResource(R.string.recurrence_monthly)
                        },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { onEvent(RegularTransactionEvent.OnShowRecurrenceTypeDialog(true)) }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.select_recurrence))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { onEvent(RegularTransactionEvent.OnShowRecurrenceTypeDialog(true)) },
                        shape = RoundedCornerShape(12.dp),
                    )

                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Text(
                            stringResource(R.string.recurrence_occurrences, uiState.recurrenceLimit),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Slider(
                            value = uiState.recurrenceLimit.toFloat(),
                            onValueChange = { onEvent(RegularTransactionEvent.OnRecurrenceLimitChange(it.toInt())) },
                            valueRange = 1f..60f,
                            steps = 59,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Text(
                            stringResource(R.string.recurrence_occurrences_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegularPaymentMethodSection(
    isPaymentMethodEnabled: Boolean,
    selectedPaymentMethodId: String?,
    allPaymentMethods: List<PaymentMethodEntity>,
    onEnabledChange: (Boolean) -> Unit,
    onMethodSelected: (String?) -> Unit,
) {
    val selectedMethod = allPaymentMethods.find { it.id == selectedPaymentMethodId }
    var showPicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.payment_method_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Switch(
                    checked = isPaymentMethodEnabled,
                    onCheckedChange = onEnabledChange,
                )
            }

            if (isPaymentMethodEnabled) {
                Spacer(modifier = Modifier.height(12.dp))

                if (allPaymentMethods.isEmpty()) {
                    Text(
                        stringResource(R.string.no_payment_methods_available),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    OutlinedTextField(
                        value = selectedMethod?.name ?: stringResource(R.string.select_payment_method),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.payment_method_label)) },
                        trailingIcon = {
                            IconButton(onClick = { showPicker = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.select_payment_method))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { showPicker = true },
                        shape = RoundedCornerShape(12.dp),
                    )

                    if (showPicker) {
                        PaymentMethodPickerDialog(
                            allPaymentMethods = allPaymentMethods,
                            currentMethodId = selectedPaymentMethodId,
                            onMethodSelected = { methodId, _ ->
                                onMethodSelected(methodId)
                                showPicker = false
                            },
                            onDismiss = { showPicker = false },
                        )
                    }
                }
            }
        }
    }
}
