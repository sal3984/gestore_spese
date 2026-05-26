package com.expense.management.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.data.CategoryEntity
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.RecurrenceType
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.PaymentProvider
import com.expense.management.domain.usecase.AddTransactionSaveResult
import com.expense.management.domain.usecase.AddTransactionSaveUseCase
import com.expense.management.ui.model.DeleteType
import com.expense.management.ui.theme.gestoreSpeseTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.ceil
import androidx.compose.ui.text.intl.Locale as ComposeLocale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AddCreditCardTransactionScreen(
    modifier: Modifier = Modifier,
    transactionToEdit: TransactionEntity?,
    currencySymbol: String,
    dateFormat: String,
    ccPaymentMode: String,
    suggestions: List<String>,
    availableCategories: List<CategoryEntity>,
    onSave: (TransactionEntity) -> Unit,
    onDelete: (String, DeleteType) -> Unit,
    onBack: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onConvertAmount: suspend (String, String, Double) -> Double? = { _, _, _ -> null },
    activeCreditCards: List<ActiveCreditCard> = emptyList(),
    allPaymentMethods: List<PaymentMethodEntity> = emptyList(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    frequentExpenseCategories: List<CategoryEntity> = emptyList(),
    frequentIncomeCategories: List<CategoryEntity> = emptyList(),
) {
    val displayFormatter = remember(dateFormat) { DateTimeFormatter.ofPattern(dateFormat) }
    val locale = ComposeLocale.current.platformLocale
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isEditing = transactionToEdit != null

    val creditCardMethods = remember(allPaymentMethods) {
        allPaymentMethods.filter {
            it.provider == PaymentProvider.CREDIT_CARD_SALDO.name ||
                it.provider == PaymentProvider.CREDIT_CARD_REVOLVING.name
        }
    }

    var uiState by remember(transactionToEdit) {
        mutableStateOf(
            AddTransactionUiState(
                type = transactionToEdit?.type ?: TransactionType.EXPENSE,
                amountText = transactionToEdit?.amount?.toString() ?: "",
                description = transactionToEdit?.description ?: "",
                selectedCategory = transactionToEdit?.categoryId.takeIf { id -> availableCategories.any { it.id == id } }
                    ?: transactionToEdit?.categoryId.takeIf { transactionToEdit != null }
                    ?: availableCategories.firstOrNull { it.type == TransactionType.EXPENSE }?.id
                    ?: "food",
                isCreditCard = true,
                creditCardId = transactionToEdit?.creditCardId ?: activeCreditCards.firstOrNull()?.id,
                selectedPaymentMethodId = transactionToEdit?.paymentMethodId,
                originalAmountText = transactionToEdit?.originalAmount?.toString() ?: "",
                originalCurrency = transactionToEdit?.originalCurrency ?: currencySymbol,
                isInstallment = (transactionToEdit?.totalInstallments ?: 1) > 1,
                isRecurrenceEnabled = transactionToEdit?.recurrenceType?.let { it != RecurrenceType.NONE } ?: false,
                recurrenceType = transactionToEdit?.recurrenceType ?: RecurrenceType.MONTHLY,
                recurrenceLimit = transactionToEdit?.recurrenceLimit ?: 12,
                installmentsCount = transactionToEdit?.totalInstallments ?: 3,
                dateStr = if (transactionToEdit != null) {
                    try {
                        LocalDate.parse(transactionToEdit.date, DateTimeFormatter.ISO_LOCAL_DATE).format(displayFormatter)
                    } catch (e: DateTimeParseException) {
                        transactionToEdit.date
                    }
                } else {
                    LocalDate.now().format(displayFormatter)
                },
                installmentStartDateStr = if (transactionToEdit == null) {
                    val defaultCard = activeCreditCards.firstOrNull()
                    val paymentDay = (defaultCard?.paymentDay ?: 15).coerceIn(1, 28)
                    LocalDate.now().plusMonths(1).withDayOfMonth(paymentDay).format(displayFormatter)
                } else {
                    LocalDate.now().format(displayFormatter)
                },
            ),
        )
    }

    LaunchedEffect(isEditing, transactionToEdit, uiState.creditCardId) {
        if (isEditing) {
            uiState = uiState.copy(isInstallment = (transactionToEdit?.totalInstallments ?: 1) > 1)
        } else {
            val selectedCard = activeCreditCards.find { it.id == uiState.creditCardId }
            uiState = uiState.copy(
                isInstallment = if (selectedCard != null) {
                    selectedCard.cardType == CreditCardType.REVOLVING
                } else {
                    ccPaymentMode == "installment"
                },
            )
        }
    }

    val errorInvalidInput = stringResource(R.string.error_invalid_input)
    val errorInvalidDateFormat = stringResource(R.string.error_invalid_date_format)
    val errorPastLimitDate = stringResource(R.string.error_past_limit_date)
    val installmentLabel = stringResource(R.string.installment)
    val errorConversionFailed = stringResource(R.string.error_conversion_failed)
    val okLabel = stringResource(R.string.ok)
    val errorNoCardSelected = stringResource(R.string.no_credit_card_selected)

    val saveUseCase = remember { AddTransactionSaveUseCase() }

    fun trySave() {
        when (
            val result = saveUseCase(
                uiState = uiState,
                transactionToEdit = transactionToEdit,
                availableCategories = availableCategories,
                activeCreditCards = activeCreditCards,
                dateFormat = dateFormat,
                locale = locale,
                installmentLabel = installmentLabel,
            )
        ) {
            is AddTransactionSaveResult.Ready -> {
                result.transactions.forEach { onSave(it) }
                onBack()
            }
            is AddTransactionSaveResult.PreviousMonthWarning -> {
                uiState = uiState.copy(showPreviousMonthAlert = true)
            }
            is AddTransactionSaveResult.Error -> {
                val message = when {
                    result.message == "error_invalid_input" -> errorInvalidInput
                    result.message == "error_invalid_date_format" -> errorInvalidDateFormat
                    result.message.startsWith("error_past_limit_date:") -> {
                        val formattedMonth = result.message.removePrefix("error_past_limit_date:")
                        String.format(errorPastLimitDate, formattedMonth)
                    }
                    result.message == "error_no_card_selected" -> errorNoCardSelected
                    else -> result.message
                }
                scope.launch { snackbarHostState.showSnackbar(message, okLabel) }
            }
        }
    }

    fun handleEvent(event: AddTransactionEvent) {
        when (event) {
            is AddTransactionEvent.OnTypeChange -> {
                val newCategory = availableCategories.firstOrNull { it.type == event.type }
                uiState = uiState.copy(
                    type = event.type,
                    selectedCategory = newCategory?.id ?: uiState.selectedCategory,
                )
            }
            is AddTransactionEvent.OnAmountChange -> uiState = uiState.copy(amountText = event.amount)
            is AddTransactionEvent.OnDescriptionChange -> {
                uiState = uiState.copy(description = event.description)
                onDescriptionChange(event.description)
            }
            is AddTransactionEvent.OnDescriptionExpandedChange -> uiState = uiState.copy(isDescriptionExpanded = event.expanded)
            is AddTransactionEvent.OnCategorySelected -> uiState = uiState.copy(selectedCategory = event.categoryId)
            is AddTransactionEvent.OnCreditCardToggle -> uiState = uiState.copy(isCreditCard = true)
            is AddTransactionEvent.OnCreditCardIdChange -> uiState = uiState.copy(creditCardId = event.creditCardId)
            is AddTransactionEvent.OnShowCreditCardDialog -> uiState = uiState.copy(showCreditCardDialog = event.show)
            is AddTransactionEvent.OnPaymentMethodSelected -> {
                val isCardCreditCard = event.isCreditCard
                uiState = uiState.copy(
                    selectedPaymentMethodId = event.paymentMethodId,
                    isCreditCard = true,
                    creditCardId = event.paymentMethodId,
                )
                uiState = uiState.copy(
                    isInstallment = if (isCardCreditCard) {
                        activeCreditCards.find { it.id == event.paymentMethodId }?.cardType == CreditCardType.REVOLVING
                    } else {
                        false
                    },
                )
            }
            is AddTransactionEvent.OnOriginalAmountChange -> uiState = uiState.copy(originalAmountText = event.amount)
            is AddTransactionEvent.OnOriginalCurrencyChange -> uiState = uiState.copy(originalCurrency = event.currency)
            is AddTransactionEvent.OnShowCurrencyDialog -> uiState = uiState.copy(showCurrencyDialog = event.show)
            is AddTransactionEvent.OnIsInstallmentChange -> uiState = uiState.copy(isInstallment = event.isInstallment)
            is AddTransactionEvent.OnRecurrenceEnabledChange -> uiState = uiState.copy(
                isRecurrenceEnabled = event.enabled,
                recurrenceType = if (event.enabled) uiState.recurrenceType else RecurrenceType.NONE,
            )
            is AddTransactionEvent.OnRecurrenceTypeChange -> uiState = uiState.copy(recurrenceType = event.recurrenceType)
            is AddTransactionEvent.OnRecurrenceLimitChange -> uiState = uiState.copy(recurrenceLimit = event.limit)
            is AddTransactionEvent.OnShowRecurrenceTypeDialog -> uiState = uiState.copy(showRecurrenceTypeDialog = event.show)
            is AddTransactionEvent.OnCalculationModeChange -> uiState = uiState.copy(calculationMode = event.mode)
            is AddTransactionEvent.OnInstallmentAmountChange -> {
                val totalAmount = uiState.amountText.toDoubleOrNull() ?: 0.0
                val installmentAmount = event.amount.toDoubleOrNull() ?: 0.0
                val newCount = if (totalAmount > 0 && installmentAmount > 0) {
                    ceil(totalAmount / installmentAmount).toInt()
                } else {
                    uiState.installmentsCount
                }
                uiState = uiState.copy(installmentAmountText = event.amount, installmentsCount = newCount)
            }
            is AddTransactionEvent.OnInstallmentsCountChange -> uiState = uiState.copy(installmentsCount = event.count)
            is AddTransactionEvent.OnDateChange -> {
                uiState = uiState.copy(dateStr = event.date)
                if (uiState.applyCcDelayToInstallments && uiState.isInstallment) {
                    val selectedDate = try {
                        LocalDate.parse(event.date, displayFormatter)
                    } catch (e: Exception) {
                        null
                    }
                    if (selectedDate != null) {
                        uiState = uiState.copy(installmentStartDateStr = selectedDate.plusMonths(1).withDayOfMonth(15).format(displayFormatter))
                    }
                }
            }
            is AddTransactionEvent.OnShowDatePicker -> uiState = uiState.copy(showDatePicker = event.show)
            is AddTransactionEvent.OnShowDeleteDialog -> uiState = uiState.copy(showDeleteDialog = event.show)
            is AddTransactionEvent.OnInstallmentStartDateChange -> uiState = uiState.copy(installmentStartDateStr = event.date)
            is AddTransactionEvent.OnShowInstallmentDatePicker -> uiState = uiState.copy(showInstallmentDatePicker = event.show)
            is AddTransactionEvent.OnShowPreviousMonthAlert -> uiState = uiState.copy(showPreviousMonthAlert = event.show)
            is AddTransactionEvent.OnApplyCcDelayChange -> {
                val newDateStr = if (event.apply) {
                    try {
                        val tDate = LocalDate.parse(uiState.dateStr, displayFormatter)
                        tDate.plusMonths(1).withDayOfMonth(15).format(displayFormatter)
                    } catch (e: Exception) {
                        uiState.dateStr
                    }
                } else {
                    uiState.dateStr
                }
                uiState = uiState.copy(applyCcDelayToInstallments = event.apply, installmentStartDateStr = newDateStr)
            }
            is AddTransactionEvent.OnIgnoreDateWarningChange -> uiState = uiState.copy(ignoreDateWarning = event.ignore)
            AddTransactionEvent.OnSave -> trySave()
            is AddTransactionEvent.OnDelete -> {
                onDelete(event.transactionId, event.deleteType)
                uiState = uiState.copy(showDeleteDialog = false)
            }
            is AddTransactionEvent.OnConvertAmount -> {
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
                title = { Text(if (transactionToEdit == null) stringResource(R.string.credit_card_transaction) else stringResource(R.string.edit_transaction), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (transactionToEdit != null) {
                        IconButton(onClick = { handleEvent(AddTransactionEvent.OnShowDeleteDialog(true)) }) {
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
                onSave = { handleEvent(AddTransactionEvent.OnSave) },
            )
        },
    ) { padding ->
        AddCreditCardContent(
            modifier = modifier,
            uiState = uiState,
            onEvent = ::handleEvent,
            isEditing = isEditing,
            transactionToEdit = transactionToEdit,
            currencySymbol = currencySymbol,
            dateFormat = dateFormat,
            suggestions = suggestions,
            availableCategories = availableCategories,
            activeCreditCards = activeCreditCards,
            creditCardMethods = creditCardMethods,
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
            onCurrencySelected = { handleEvent(AddTransactionEvent.OnOriginalCurrencyChange(it)) },
            onDismiss = { handleEvent(AddTransactionEvent.OnShowCurrencyDialog(false)) },
        )
    }

    if (uiState.showCreditCardDialog) {
        val legacyAsPaymentMethods = remember(activeCreditCards, creditCardMethods) {
            activeCreditCards
                .filter { card -> creditCardMethods.none { it.id == card.id } }
                .map { card ->
                    PaymentMethodEntity(
                        id = card.id,
                        name = card.name,
                        provider = card.provider.name,
                        isActive = true,
                    )
                }
        }
        val allMethodsForDialog = creditCardMethods + legacyAsPaymentMethods

        if (allMethodsForDialog.isNotEmpty()) {
            PaymentMethodPickerDialog(
                allPaymentMethods = allMethodsForDialog,
                currentMethodId = uiState.selectedPaymentMethodId ?: uiState.creditCardId,
                onMethodSelected = { methodId, isCreditCard ->
                    handleEvent(AddTransactionEvent.OnPaymentMethodSelected(methodId, isCreditCard))
                    handleEvent(
                        AddTransactionEvent.OnIsInstallmentChange(
                            if (isCreditCard) {
                                activeCreditCards.find { it.id == methodId }?.cardType == CreditCardType.REVOLVING
                            } else {
                                false
                            },
                        ),
                    )
                    handleEvent(AddTransactionEvent.OnShowCreditCardDialog(false))
                },
                onDismiss = { handleEvent(AddTransactionEvent.OnShowCreditCardDialog(false)) },
            )
        } else if (activeCreditCards.isNotEmpty()) {
            CreditCardDialog(
                activeCreditCards = activeCreditCards,
                currentCardId = uiState.creditCardId,
                onCardSelected = { cardId, isRevolving ->
                    handleEvent(AddTransactionEvent.OnCreditCardIdChange(cardId))
                    handleEvent(AddTransactionEvent.OnIsInstallmentChange(isRevolving))
                    handleEvent(AddTransactionEvent.OnShowCreditCardDialog(false))
                },
                onDismiss = { handleEvent(AddTransactionEvent.OnShowCreditCardDialog(false)) },
            )
        }
    }

    if (uiState.showRecurrenceTypeDialog) {
        RecurrenceTypeDialog(
            currentRecurrence = uiState.recurrenceType,
            onRecurrenceSelected = { handleEvent(AddTransactionEvent.OnRecurrenceTypeChange(it)) },
            onDismiss = { handleEvent(AddTransactionEvent.OnShowRecurrenceTypeDialog(false)) },
        )
    }

    if (uiState.showPreviousMonthAlert) {
        PreviousMonthDialog(
            onConfirm = {
                handleEvent(AddTransactionEvent.OnIgnoreDateWarningChange(true))
                trySave()
            },
            onDismiss = { handleEvent(AddTransactionEvent.OnShowPreviousMonthAlert(false)) },
        )
    }

    if (uiState.showDatePicker) {
        TransactionDatePicker(
            currentDate = uiState.dateStr,
            dateFormat = dateFormat,
            onDateSelected = { handleEvent(AddTransactionEvent.OnDateChange(it)) },
            onDismiss = { handleEvent(AddTransactionEvent.OnShowDatePicker(false)) },
        )
    }

    if (uiState.showInstallmentDatePicker) {
        InstallmentDatePicker(
            currentDate = uiState.installmentStartDateStr,
            dateFormat = dateFormat,
            onDateSelected = { handleEvent(AddTransactionEvent.OnInstallmentStartDateChange(it)) },
            onDismiss = { handleEvent(AddTransactionEvent.OnShowInstallmentDatePicker(false)) },
        )
    }

    if (uiState.showDeleteDialog) {
        DeleteDialog(
            transactionToEdit = transactionToEdit,
            onDelete = { id, type -> handleEvent(AddTransactionEvent.OnDelete(id, type)) },
            onDismiss = { handleEvent(AddTransactionEvent.OnShowDeleteDialog(false)) },
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AddCreditCardContent(
    modifier: Modifier = Modifier,
    uiState: AddTransactionUiState,
    onEvent: (AddTransactionEvent) -> Unit,
    isEditing: Boolean,
    transactionToEdit: TransactionEntity?,
    currencySymbol: String,
    dateFormat: String,
    suggestions: List<String>,
    availableCategories: List<CategoryEntity>,
    activeCreditCards: List<ActiveCreditCard>,
    creditCardMethods: List<PaymentMethodEntity>,
    frequentExpenseCategories: List<CategoryEntity>,
    frequentIncomeCategories: List<CategoryEntity>,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
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
            onTypeChange = { onEvent(AddTransactionEvent.OnTypeChange(it)) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        BasicDetailsCard(
            uiState = uiState,
            onEvent = onEvent,
            transactionToEdit = transactionToEdit,
            currencySymbol = currencySymbol,
            dateFormat = dateFormat,
            suggestions = suggestions,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )

        if (!isEditing) {
            Spacer(modifier = Modifier.height(12.dp))
            RecurrenceSection(
                uiState = uiState,
                onEvent = onEvent,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        CreditCardPaymentSection(
            uiState = uiState,
            onEvent = onEvent,
            isEditing = isEditing,
            transactionToEdit = transactionToEdit,
            activeCreditCards = activeCreditCards,
            creditCardMethods = creditCardMethods,
        )

        Spacer(modifier = Modifier.height(12.dp))

        InstallmentSection(
            uiState = uiState,
            onEvent = onEvent,
            isEditing = isEditing,
            transactionToEdit = transactionToEdit,
            currencySymbol = currencySymbol,
            dateFormat = dateFormat,
            activeCreditCards = activeCreditCards,
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
            onCategorySelected = { onEvent(AddTransactionEvent.OnCategorySelected(it)) },
            availableCategories = availableCategories,
            frequentExpenseCategories = frequentExpenseCategories,
            frequentIncomeCategories = frequentIncomeCategories,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardPaymentSection(
    uiState: AddTransactionUiState,
    onEvent: (AddTransactionEvent) -> Unit,
    isEditing: Boolean,
    transactionToEdit: TransactionEntity?,
    activeCreditCards: List<ActiveCreditCard>,
    creditCardMethods: List<PaymentMethodEntity>,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.credit_card),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            if (creditCardMethods.isNotEmpty() || activeCreditCards.isNotEmpty()) {
                val selectedName = when {
                    uiState.selectedPaymentMethodId != null ->
                        creditCardMethods.find { it.id == uiState.selectedPaymentMethodId }?.name
                            ?: activeCreditCards.find { it.id == uiState.selectedPaymentMethodId }?.name
                    uiState.creditCardId != null ->
                        activeCreditCards.find { it.id == uiState.creditCardId }?.name
                            ?: creditCardMethods.find { it.id == uiState.creditCardId }?.name
                    else -> null
                }
                OutlinedTextField(
                    value = selectedName ?: stringResource(R.string.select_credit_card),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.credit_card)) },
                    trailingIcon = {
                        IconButton(onClick = { onEvent(AddTransactionEvent.OnShowCreditCardDialog(true)) }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.select_payment_method))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { onEvent(AddTransactionEvent.OnShowCreditCardDialog(true)) },
                )
            }

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
                    onCheckedChange = { if (!isEditing) onEvent(AddTransactionEvent.OnIsInstallmentChange(it)) },
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

@Preview(showBackground = true, name = "AddCreditCard Light")
@Composable
private fun AddCreditCardPreview() {
    gestoreSpeseTheme(darkTheme = false, dynamicColor = false) {
        AddCreditCardTransactionScreen(transactionToEdit = null, currencySymbol = "\u20AC", dateFormat = "dd/MM/yyyy", ccPaymentMode = "single", suggestions = emptyList(), availableCategories = emptyList(), onSave = {}, onDelete = { _, _ -> }, onBack = {}, onDescriptionChange = {})
    }
}

@Preview(showBackground = true, name = "AddCreditCard Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddCreditCardPreviewDark() {
    gestoreSpeseTheme(darkTheme = true, dynamicColor = false) {
        AddCreditCardTransactionScreen(transactionToEdit = null, currencySymbol = "\u20AC", dateFormat = "dd/MM/yyyy", ccPaymentMode = "single", suggestions = emptyList(), availableCategories = emptyList(), onSave = {}, onDelete = { _, _ -> }, onBack = {}, onDescriptionChange = {})
    }
}
