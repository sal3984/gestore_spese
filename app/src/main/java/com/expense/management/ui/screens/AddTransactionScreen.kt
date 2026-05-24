package com.expense.management.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.data.CardType
import com.expense.management.data.CategoryEntity
import com.expense.management.data.CreditCardEntity
import com.expense.management.data.RecurrenceType
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.ui.model.DeleteType
import com.expense.management.ui.theme.gestoreSpeseTheme
import com.expense.management.utils.CategoryImage
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.ceil
import androidx.compose.ui.text.intl.Locale as ComposeLocale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AddTransactionScreen(
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
    isCC: Boolean = false,
    availableCreditCards: List<CreditCardEntity> = emptyList(),
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
                isCreditCard = transactionToEdit?.isCreditCard ?: isCC,
                creditCardId = transactionToEdit?.creditCardId ?: availableCreditCards.firstOrNull()?.id,
                originalAmountText = transactionToEdit?.originalAmount?.toString() ?: "",
                originalCurrency = transactionToEdit?.originalCurrency ?: currencySymbol,
                isInstallment = (transactionToEdit?.totalInstallments ?: 1) > 1,
                recurrenceType = transactionToEdit?.recurrenceType ?: RecurrenceType.NONE,
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
                installmentStartDateStr = if (transactionToEdit == null && isCC) {
                    LocalDate.now().plusMonths(1).withDayOfMonth(15).format(displayFormatter)
                } else {
                    LocalDate.now().format(displayFormatter)
                },
            )
        )
    }

    LaunchedEffect(isEditing, transactionToEdit, uiState.isCreditCard, ccPaymentMode, uiState.creditCardId) {
        if (isEditing) {
            uiState = uiState.copy(isInstallment = (transactionToEdit?.totalInstallments ?: 1) > 1)
        } else {
            if (uiState.isCreditCard) {
                val selectedCard = availableCreditCards.find { it.id == uiState.creditCardId }
                uiState = uiState.copy(
                    isInstallment = if (selectedCard != null) {
                        selectedCard.type == CardType.REVOLVING
                    } else {
                        ccPaymentMode == "installment"
                    }
                )
            } else {
                uiState = uiState.copy(isInstallment = false)
            }
        }
    }

    val errorInvalidInput = stringResource(R.string.error_invalid_input)
    val errorInvalidDateFormat = stringResource(R.string.error_invalid_date_format)
    val errorPastLimitDate = stringResource(R.string.error_past_limit_date)
    val installmentLabel = stringResource(R.string.installment)
    val errorConversionFailed = stringResource(R.string.error_conversion_failed)
    val okLabel = stringResource(R.string.ok)

    val saveUseCase = remember { AddTransactionSaveUseCase() }

    fun trySave() {
        when (val result = saveUseCase(
            uiState = uiState,
            transactionToEdit = transactionToEdit,
            availableCategories = availableCategories,
            availableCreditCards = availableCreditCards,
            isCC = isCC,
            dateFormat = dateFormat,
            locale = locale,
            installmentLabel = installmentLabel,
        )) {
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
                    result.message == "error_no_card_selected" -> "Nessuna carta selezionata. Crea o seleziona una carta di credito."
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
                    selectedCategory = newCategory?.id ?: uiState.selectedCategory
                )
            }
            is AddTransactionEvent.OnAmountChange -> uiState = uiState.copy(amountText = event.amount)
            is AddTransactionEvent.OnDescriptionChange -> {
                uiState = uiState.copy(description = event.description)
                onDescriptionChange(event.description)
            }
            is AddTransactionEvent.OnDescriptionExpandedChange -> uiState = uiState.copy(isDescriptionExpanded = event.expanded)
            is AddTransactionEvent.OnCategorySelected -> uiState = uiState.copy(selectedCategory = event.categoryId)
            is AddTransactionEvent.OnCreditCardToggle -> uiState = uiState.copy(isCreditCard = event.isCreditCard)
            is AddTransactionEvent.OnCreditCardIdChange -> uiState = uiState.copy(creditCardId = event.creditCardId)
            is AddTransactionEvent.OnShowCreditCardDialog -> uiState = uiState.copy(showCreditCardDialog = event.show)
            is AddTransactionEvent.OnOriginalAmountChange -> uiState = uiState.copy(originalAmountText = event.amount)
            is AddTransactionEvent.OnOriginalCurrencyChange -> uiState = uiState.copy(originalCurrency = event.currency)
            is AddTransactionEvent.OnShowCurrencyDialog -> uiState = uiState.copy(showCurrencyDialog = event.show)
            is AddTransactionEvent.OnIsInstallmentChange -> uiState = uiState.copy(isInstallment = event.isInstallment)
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
                if (uiState.applyCcDelayToInstallments && uiState.isCreditCard && uiState.isInstallment) {
                    val selectedDate = try {
                        LocalDate.parse(event.date, displayFormatter)
                    } catch (e: Exception) {
                        null
                    }
                    if (selectedDate != null) {
                        uiState = uiState.copy(installmentStartDateStr = selectedDate.plusMonths(1).withDayOfMonth(15).format(displayFormatter))
                    }
                } else if (!uiState.isCreditCard && uiState.isInstallment) {
                    uiState = uiState.copy(installmentStartDateStr = event.date)
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
                title = { Text(if (transactionToEdit == null) stringResource(R.string.add_transaction) else stringResource(R.string.edit_transaction), fontWeight = FontWeight.SemiBold) },
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
        AddTransactionContent(
            modifier = modifier,
            uiState = uiState,
            onEvent = ::handleEvent,
            isEditing = isEditing,
            transactionToEdit = transactionToEdit,
            currencySymbol = currencySymbol,
            dateFormat = dateFormat,
            suggestions = suggestions,
            availableCategories = availableCategories,
            availableCreditCards = availableCreditCards,
            frequentExpenseCategories = frequentExpenseCategories,
            frequentIncomeCategories = frequentIncomeCategories,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            padding = padding,
            isCC = isCC,
        )
    }

    if (uiState.showCurrencyDialog) {
        CurrencyDialog(
            currentCurrency = uiState.originalCurrency,
            onCurrencySelected = { handleEvent(AddTransactionEvent.OnOriginalCurrencyChange(it)) },
            onDismiss = { handleEvent(AddTransactionEvent.OnShowCurrencyDialog(false)) },
        )
    }

    if (uiState.showCreditCardDialog && availableCreditCards.isNotEmpty()) {
        CreditCardDialog(
            availableCreditCards = availableCreditCards,
            currentCardId = uiState.creditCardId,
            onCardSelected = { cardId, isRevolving ->
                handleEvent(AddTransactionEvent.OnCreditCardIdChange(cardId))
                handleEvent(AddTransactionEvent.OnIsInstallmentChange(isRevolving))
                handleEvent(AddTransactionEvent.OnShowCreditCardDialog(false))
            },
            onDismiss = { handleEvent(AddTransactionEvent.OnShowCreditCardDialog(false)) },
        )
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
private fun AddTransactionContent(
    modifier: Modifier = Modifier,
    uiState: AddTransactionUiState,
    onEvent: (AddTransactionEvent) -> Unit,
    isEditing: Boolean,
    transactionToEdit: TransactionEntity?,
    currencySymbol: String,
    dateFormat: String,
    suggestions: List<String>,
    availableCategories: List<CategoryEntity>,
    availableCreditCards: List<CreditCardEntity>,
    frequentExpenseCategories: List<CategoryEntity>,
    frequentIncomeCategories: List<CategoryEntity>,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    padding: PaddingValues,
    isCC: Boolean = false,
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

        Spacer(modifier = Modifier.height(12.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

        if (!isEditing) {
            RecurrenceSection(
                uiState = uiState,
                onEvent = onEvent,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        PaymentMethodSection(
            uiState = uiState,
            onEvent = onEvent,
            isEditing = isEditing,
            transactionToEdit = transactionToEdit,
            availableCreditCards = availableCreditCards,
            isCC = isCC,
        )

        Spacer(modifier = Modifier.height(16.dp))

        InstallmentSection(
            uiState = uiState,
            onEvent = onEvent,
            isEditing = isEditing,
            transactionToEdit = transactionToEdit,
            currencySymbol = currencySymbol,
            dateFormat = dateFormat,
            availableCreditCards = availableCreditCards,
        )
    }
}

@Composable
fun CategorySelector(
    type: TransactionType,
    selectedCategoryId: String?,
    onCategorySelected: (String) -> Unit,
    availableCategories: List<CategoryEntity>,
    frequentExpenseCategories: List<CategoryEntity> = emptyList(),
    frequentIncomeCategories: List<CategoryEntity> = emptyList(),
) {
    val frequentCategories = remember(type, frequentExpenseCategories, frequentIncomeCategories) {
        if (type == TransactionType.EXPENSE) frequentExpenseCategories else frequentIncomeCategories
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredCategories = remember(availableCategories, type, searchQuery) {
        availableCategories.filter { it.type == type && it.label.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.search_categories)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        )

        if (frequentCategories.isNotEmpty() && searchQuery.isEmpty()) {
            Text(
                text = stringResource(R.string.frequent_categories),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                frequentCategories.forEach { category ->
                    key(category.id) {
                        CategoryChip(
                            category = category,
                            isSelected = selectedCategoryId == category.id,
                            onClick = { onCategorySelected(category.id) },
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
        }

        Text(
            text = if (searchQuery.isEmpty()) stringResource(R.string.all_categories) else stringResource(R.string.search_results),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            if (filteredCategories.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_categories_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val chunks = remember(filteredCategories) { filteredCategories.chunked(4) }
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
                                        onClick = { onCategorySelected(category.id) },
                                    )
                                }
                            }
                            if (rowCategories.size < 4) {
                                repeat(4 - rowCategories.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: CategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.height(40.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            CategoryImage(category = category, size = 24.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = category.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

@Composable
fun CategoryGridItem(
    category: CategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                    shape = RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            CategoryImage(category = category, size = 32.dp)
        }
        Text(
            text = category.label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Preview(showBackground = true, name = "AddTransaction Light")
@Composable
private fun AddTransactionPreview() {
    gestoreSpeseTheme(darkTheme = false, dynamicColor = false) {
        AddTransactionScreen(transactionToEdit = null, currencySymbol = "\u20AC", dateFormat = "dd/MM/yyyy", ccPaymentMode = "single", suggestions = emptyList(), availableCategories = emptyList(), onSave = {}, onDelete = { _, _ -> }, onBack = {}, onDescriptionChange = {})
    }
}

@Preview(showBackground = true, name = "AddTransaction Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddTransactionPreviewDark() {
    gestoreSpeseTheme(darkTheme = true, dynamicColor = false) {
        AddTransactionScreen(transactionToEdit = null, currencySymbol = "\u20AC", dateFormat = "dd/MM/yyyy", ccPaymentMode = "single", suggestions = emptyList(), availableCategories = emptyList(), onSave = {}, onDelete = { _, _ -> }, onBack = {}, onDescriptionChange = {})
    }
}
