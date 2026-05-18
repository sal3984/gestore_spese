package com.expense.management.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expense.management.R
import com.expense.management.data.CardType
import com.expense.management.data.CategoryEntity
import com.expense.management.data.CreditCardEntity
import com.expense.management.data.RecurrenceType
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.ui.model.DeleteType
import com.expense.management.utils.CategoryImage
import com.expense.management.utils.DateUtils
import com.expense.management.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.floor
import androidx.compose.ui.text.intl.Locale as ComposeLocale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AddTransactionScreen(
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
    viewModel: ExpenseViewModel? = null,
) {
    val displayFormatter = remember(dateFormat) { DateTimeFormatter.ofPattern(dateFormat) }
    val locale = ComposeLocale.current.platformLocale
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var type by remember { mutableStateOf(transactionToEdit?.type ?: TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(transactionToEdit?.description ?: "") }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    val currentTypeCategories = remember(availableCategories, type) {
        availableCategories.filter { it.type == type }
    }

    var selectedCategory by remember(type, currentTypeCategories) {
        mutableStateOf(
            transactionToEdit?.categoryId.takeIf { id -> availableCategories.any { it.id == id } }
                ?: transactionToEdit?.categoryId.takeIf { transactionToEdit != null }
                ?: currentTypeCategories.firstOrNull()?.id
                ?: if (type == TransactionType.EXPENSE) "food" else "salary",
        )
    }

    var isCreditCard by remember { mutableStateOf(transactionToEdit?.isCreditCard ?: isCC) }
    var creditCardId by remember { mutableStateOf(transactionToEdit?.creditCardId ?: availableCreditCards.firstOrNull()?.id) }
    var showCreditCardDialog by remember { mutableStateOf(false) }

    var originalAmountText by remember { mutableStateOf(transactionToEdit?.originalAmount?.toString() ?: "") }
    var originalCurrency by remember { mutableStateOf(transactionToEdit?.originalCurrency ?: currencySymbol) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var isConverting by remember { mutableStateOf(false) } // State for loading indicator

    var isInstallment by remember { mutableStateOf(false) }
    val isEditing = transactionToEdit != null

    // --- Recurrence States ---
    var recurrenceType by remember { mutableStateOf(transactionToEdit?.recurrenceType ?: RecurrenceType.NONE) }
    var recurrenceLimit by remember { mutableIntStateOf(transactionToEdit?.recurrenceLimit ?: 12) }
    var showRecurrenceTypeDialog by remember { mutableStateOf(false) }

    // --- Installment States ---
    var calculationMode by remember { mutableStateOf("installments") } // "installments" or "amount"
    var installmentAmountText by remember { mutableStateOf("") }
    var installmentsCount by remember {
        mutableIntStateOf(transactionToEdit?.totalInstallments ?: 3)
    }

    LaunchedEffect(isEditing, transactionToEdit, isCreditCard, ccPaymentMode, creditCardId) {
        if (isEditing) {
            isInstallment = (transactionToEdit.totalInstallments ?: 1) > 1
        } else {
            if (isCreditCard) {
                // Se è una carta configurata, usiamo il suo tipo per determinare se è rateale
                val selectedCard = availableCreditCards.find { it.id == creditCardId }
                if (selectedCard != null) {
                    isInstallment = selectedCard.type == CardType.REVOLVING
                } else {
                    // Fallback alle impostazioni globali se non c'è una carta specifica o non trovata
                    isInstallment = when (ccPaymentMode) {
                        "installment" -> true
                        else -> false
                    }
                }
            } else {
                isInstallment = false
            }
        }
    }

    var applyCcDelayToInstallments by remember { mutableStateOf(true) }
    var ignoreDateWarning by remember { mutableStateOf(false) }

    var dateStr by remember {
        mutableStateOf(
            if (transactionToEdit != null) {
                try {
                    LocalDate.parse(transactionToEdit.date, DateTimeFormatter.ISO_LOCAL_DATE).format(displayFormatter)
                } catch (e: DateTimeParseException) {
                    transactionToEdit.date
                }
            } else {
                LocalDate.now().format(displayFormatter)
            },
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var installmentStartDateStr by remember {
        mutableStateOf(
            if (transactionToEdit == null && applyCcDelayToInstallments && isCreditCard) {
                LocalDate.now().plusMonths(1).withDayOfMonth(15).format(displayFormatter)
            } else {
                LocalDate.now().format(displayFormatter)
            },
        )
    }
    var showInstallmentDatePicker by remember { mutableStateOf(false) }
    var showPreviousMonthAlert by remember { mutableStateOf(false) }

    val errorInvalidInput = stringResource(R.string.error_invalid_input)
    val errorInvalidDateFormat = stringResource(R.string.error_invalid_date_format)
    val errorPastLimitDate = stringResource(R.string.error_past_limit_date)
    val installmentLabel = stringResource(R.string.installment)
    val errorConversionFailed = stringResource(R.string.error_conversion_failed)

    val selectedCard = availableCreditCards.find { it.id == creditCardId }

    fun trySave() {
        val amount = amountText.toDoubleOrNull() ?: 0.0
        val originalAmount = originalAmountText.toDoubleOrNull() ?: amount

        if (amount <= 0 || description.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar(errorInvalidInput, "OK") }
            return
        }

        val transactionDate = try {
            LocalDate.parse(dateStr, displayFormatter)
        } catch (e: DateTimeParseException) {
            scope.launch { snackbarHostState.showSnackbar(errorInvalidDateFormat, "OK") }
            return
        }

        val transactionId = transactionToEdit?.id ?: UUID.randomUUID().toString()
        val limitMonth = YearMonth.now().minusMonths(1)
        val transactionMonth = YearMonth.from(transactionDate)

        if (transactionMonth.isBefore(limitMonth)) {
            scope.launch {
                val formattedMonth = limitMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
                snackbarHostState.showSnackbar(String.format(errorPastLimitDate, formattedMonth), "OK")
            }
            return
        }

        if (transactionMonth.isBefore(YearMonth.now()) && transactionToEdit == null && !ignoreDateWarning) {
            showPreviousMonthAlert = true
            return
        }

        val dateToSave = transactionDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        if (isInstallment && transactionToEdit == null) {
            val installmentAmountFromField = installmentAmountText.toDoubleOrNull() ?: 0.0

            val finalInstallmentsCount = if (calculationMode == "amount" && installmentAmountFromField > 0) {
                ceil(amount / installmentAmountFromField).toInt()
            } else {
                installmentsCount
            }

            if (finalInstallmentsCount <= 0) return

            val groupId = UUID.randomUUID().toString()
            val startInstallmentDate = try {
                LocalDate.parse(installmentStartDateStr, displayFormatter)
            } catch (e: DateTimeParseException) {
                transactionDate
            }

            for (i in 0 until finalInstallmentsCount) {
                val installmentDate = startInstallmentDate.plusMonths(i.toLong())
                val settlementDate = if (isCreditCard && selectedCard != null) {
                    DateUtils.calculateEffectiveDate(installmentDate, selectedCard)
                } else {
                    installmentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                }

                val effectiveDate = if (isCreditCard && type == TransactionType.INCOME) {
                    installmentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                } else if (isCreditCard && applyCcDelayToInstallments) {
                    settlementDate
                } else {
                    installmentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                }

                val newId = if (i == 0) transactionId else UUID.randomUUID().toString()
                val installmentDateToSave = installmentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                val currentInstallmentAmount: Double
                val currentOriginalInstallmentAmount: Double
                val originalRatio = if (amount > 0) originalAmount / amount else 1.0

                if (calculationMode == "amount" && installmentAmountFromField > 0) {
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

                if (isCreditCard && type == TransactionType.INCOME) {
                    val expenseCategoryId = availableCategories.find { it.id == "credit_card_payment" }?.id
                        ?: availableCategories.firstOrNull { it.type == TransactionType.EXPENSE }?.id
                        ?: "other"

                    onSave(
                        TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            date = installmentDateToSave,
                            description = "[${selectedCard?.name ?: "Credit Card"}] ${description.trim()} ($installmentLabel ${i + 1}/$finalInstallmentsCount) (Future Payment)",
                            amount = currentInstallmentAmount,
                            categoryId = expenseCategoryId,
                            type = TransactionType.EXPENSE,
                            isCreditCard = false,
                            originalAmount = currentOriginalInstallmentAmount,
                            originalCurrency = originalCurrency,
                            effectiveDate = settlementDate,
                            creditCardId = null,
                            groupId = groupId,
                        ),
                    )
                }

                onSave(
                    TransactionEntity(
                        id = newId,
                        date = installmentDateToSave,
                        description = "$description ($installmentLabel ${i + 1}/$finalInstallmentsCount)",
                        amount = currentInstallmentAmount,
                        categoryId = selectedCategory,
                        type = type,
                        isCreditCard = isCreditCard,
                        originalAmount = currentOriginalInstallmentAmount,
                        originalCurrency = originalCurrency,
                        effectiveDate = effectiveDate,
                        installmentNumber = i + 1,
                        totalInstallments = finalInstallmentsCount,
                        groupId = groupId,
                        creditCardId = creditCardId,
                    ),
                )
            }
        } else if (recurrenceType != RecurrenceType.NONE && transactionToEdit == null) {
            val groupId = UUID.randomUUID().toString()
            var currentOccurrenceDate = transactionDate

            for (count in 0 until recurrenceLimit) {
                val settlementDate = if (isCreditCard && selectedCard != null) {
                    DateUtils.calculateEffectiveDate(currentOccurrenceDate, selectedCard)
                } else {
                    currentOccurrenceDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                }

                val effectiveDate = if (isCreditCard && type == TransactionType.INCOME) {
                    currentOccurrenceDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                } else {
                    settlementDate
                }

                val newId = if (count == 0) transactionId else UUID.randomUUID().toString()
                val occurrenceDateStr = currentOccurrenceDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                if (isCreditCard && type == TransactionType.INCOME) {
                    val expenseCategoryId = availableCategories.find { it.id == "credit_card_payment" }?.id
                        ?: availableCategories.firstOrNull { it.type == TransactionType.EXPENSE }?.id
                        ?: "other"

                    onSave(
                        TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            date = occurrenceDateStr,
                            description = "[${selectedCard?.name ?: "Credit Card"}] ${description.trim()} (Future Payment)",
                            amount = amount,
                            categoryId = expenseCategoryId,
                            type = TransactionType.EXPENSE,
                            isCreditCard = false,
                            originalAmount = originalAmount,
                            originalCurrency = originalCurrency,
                            effectiveDate = settlementDate,
                            creditCardId = null,
                            groupId = groupId,
                        ),
                    )
                }

                onSave(
                    TransactionEntity(
                        id = newId,
                        date = occurrenceDateStr,
                        description = description.trim(),
                        amount = amount,
                        categoryId = selectedCategory,
                        type = type,
                        isCreditCard = isCreditCard,
                        originalAmount = originalAmount,
                        originalCurrency = originalCurrency,
                        effectiveDate = effectiveDate,
                        groupId = groupId,
                        creditCardId = if (isCreditCard) creditCardId else null,
                        recurrenceType = recurrenceType,
                        recurrenceLimit = recurrenceLimit,
                    ),
                )

                currentOccurrenceDate = when (recurrenceType) {
                    RecurrenceType.DAILY -> currentOccurrenceDate.plusDays(1)
                    RecurrenceType.WEEKLY -> currentOccurrenceDate.plusWeeks(1)
                    RecurrenceType.MONTHLY -> currentOccurrenceDate.plusMonths(1)
                    RecurrenceType.YEARLY -> currentOccurrenceDate.plusYears(1)
                    else -> break
                }
            }
        } else {
            if (selectedCard == null) {
                if (isCreditCard) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Nessuna carta selezionata. Crea o seleziona una carta di credito.",
                            withDismissAction = true,
                        )
                    }
                    return
                }
            }

            val settlementDate = if (isCreditCard && selectedCard != null) {
                DateUtils.calculateEffectiveDate(transactionDate, selectedCard)
            } else {
                transactionDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            }

            // Per le entrate (ricariche) con carta, l'effetto sulla liquidità è immediato (dateToSave),
            // ma il debito sulla carta sarà saldato alla data di regolamento (settlementDate).
            // Per le uscite con carta, l'effetto sulla liquidità è posticipato (settlementDate).
            val effectiveDate = if (isCreditCard && type == TransactionType.INCOME) {
                dateToSave
            } else {
                settlementDate
            }

            val commonGroupId = if (isCreditCard && !isInstallment && transactionToEdit == null) {
                UUID.randomUUID().toString()
            } else {
                transactionToEdit?.groupId
            }

            // Gestione doppia registrazione per Carte di Credito (non a rate)
            if (isCreditCard && !isInstallment && transactionToEdit == null) {
                if (type == TransactionType.INCOME) {
                    // RICARICA con CC: Registriamo un'uscita tecnica alla data di saldo della carta (settlementDate)
                    val expenseCategoryId = availableCategories.find { it.id == "credit_card_payment" }?.id
                        ?: availableCategories.firstOrNull { it.type == TransactionType.EXPENSE }?.id
                        ?: "other"

                    onSave(
                        TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            date = dateToSave,
                            description = "[${selectedCard?.name ?: "Credit Card"}] ${description.trim()} (Future Payment)",
                            amount = amount,
                            categoryId = expenseCategoryId,
                            type = TransactionType.EXPENSE,
                            isCreditCard = false,
                            originalAmount = originalAmount,
                            originalCurrency = originalCurrency,
                            effectiveDate = settlementDate,
                            creditCardId = null,
                            groupId = commonGroupId,
                        ),
                    )
                }
            }

            onSave(
                TransactionEntity(
                    id = transactionId,
                    date = dateToSave,
                    description = description.trim(),
                    amount = amount,
                    categoryId = selectedCategory,
                    type = type,
                    isCreditCard = isCreditCard,
                    originalAmount = originalAmount,
                    originalCurrency = originalCurrency,
                    effectiveDate = effectiveDate,
                    installmentNumber = transactionToEdit?.installmentNumber,
                    totalInstallments = transactionToEdit?.totalInstallments,
                    groupId = commonGroupId,
                    creditCardId = if (isCreditCard) creditCardId else null,
                    recurrenceType = transactionToEdit?.recurrenceType ?: RecurrenceType.NONE,
                    recurrenceLimit = transactionToEdit?.recurrenceLimit,
                ),
            )
        }
        onBack()
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
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 16.dp,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                ) {
                    Button(
                        onClick = { trySave() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (transactionToEdit == null) {
                                stringResource(R.string.save_transaction)
                            } else {
                                stringResource(R.string.update_transaction)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            ) {
                val expenseSelected = type == TransactionType.EXPENSE
                SegmentedButton(
                    selected = expenseSelected,
                    onClick = {
                        if (type != TransactionType.EXPENSE) {
                            type = TransactionType.EXPENSE
                            val newCategory = availableCategories.firstOrNull { it.type == TransactionType.EXPENSE }
                            if (newCategory != null) selectedCategory = newCategory.id
                        }
                    },
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
                    onClick = {
                        if (type != TransactionType.INCOME) {
                            type = TransactionType.INCOME
                            val newCategory = availableCategories.firstOrNull { it.type == TransactionType.INCOME }
                            if (newCategory != null) selectedCategory = newCategory.id
                        }
                    },
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

            Spacer(modifier = Modifier.height(12.dp))

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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.basic_details_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )

                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text(stringResource(R.string.transaction_date)) },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.select_date_desc))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ExposedDropdownMenuBox(
                        expanded = isDescriptionExpanded && suggestions.isNotEmpty(),
                        onExpandedChange = { isDescriptionExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { newText ->
                                description = newText
                                isDescriptionExpanded = true
                                onDescriptionChange(newText)
                            },
                            label = { Text(stringResource(R.string.description)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                if (description.isNotEmpty()) {
                                    IconButton(onClick = {
                                        description = ""
                                        onDescriptionChange("")
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        )
                        ExposedDropdownMenu(
                            expanded = isDescriptionExpanded && suggestions.isNotEmpty(),
                            onDismissRequest = { isDescriptionExpanded = false },
                        ) {
                            suggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(text = suggestion) },
                                    onClick = {
                                        description = suggestion
                                        isDescriptionExpanded = false
                                        onDescriptionChange("")
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (originalCurrency == currencySymbol) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            TextButton(
                                onClick = { showCurrencyDialog = true },
                            ) {
                                Text(stringResource(R.string.set_original_currency))
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    AnimatedVisibility(visible = originalCurrency != currencySymbol) {
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
                                        value = originalAmountText,
                                        onValueChange = { originalAmountText = it.replace(',', '.') },
                                        label = { Text(stringResource(R.string.amount_original_label)) },
                                        placeholder = { Text("0.00") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))

                                    OutlinedTextField(
                                        value = originalCurrency,
                                        onValueChange = { originalCurrency = it.uppercase(Locale.ROOT) },
                                        label = { Text(stringResource(R.string.currency_original_label)) },
                                        readOnly = true,
                                        trailingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        modifier = Modifier
                                            .weight(0.7f)
                                            .clickable { showCurrencyDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                }

                                // Convert Button (moved below the fields)
                                Button(
                                    onClick = {
                                        val amount = originalAmountText.toDoubleOrNull()
                                        if (amount != null && amount > 0) {
                                            scope.launch {
                                                isConverting = true
                                                val result = onConvertAmount(originalCurrency, currencySymbol, amount)
                                                isConverting = false
                                                if (result != null) {
                                                    amountText = String.format(Locale.US, "%.2f", result)
                                                } else {
                                                    snackbarHostState.showSnackbar(errorConversionFailed)
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isConverting && originalAmountText.isNotEmpty(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                ) {
                                    if (isConverting) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.converting))
                                    } else {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null)
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
                        value = amountText,
                        onValueChange = { amountText = it.replace(',', '.') },
                        label = { Text(stringResource(R.string.amount_converted_label, currencySymbol)) },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }

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
                type = type,
                selectedCategoryId = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                availableCategories = availableCategories,
                viewModel = viewModel,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Recurrence Section ---
            if (!isEditing) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.recurrence_label),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )

                        OutlinedTextField(
                            value = when (recurrenceType) {
                                RecurrenceType.NONE -> stringResource(R.string.recurrence_none)
                                RecurrenceType.DAILY -> stringResource(R.string.recurrence_daily)
                                RecurrenceType.WEEKLY -> stringResource(R.string.recurrence_weekly)
                                RecurrenceType.MONTHLY -> stringResource(R.string.recurrence_monthly)
                                RecurrenceType.YEARLY -> stringResource(R.string.recurrence_yearly)
                            },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showRecurrenceTypeDialog = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { showRecurrenceTypeDialog = true },
                            shape = RoundedCornerShape(12.dp),
                        )

                        AnimatedVisibility(visible = recurrenceType != RecurrenceType.NONE) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                Text(
                                    text = stringResource(R.string.recurrence_occurrences, recurrenceLimit),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Slider(
                                    value = recurrenceLimit.toFloat(),
                                    onValueChange = { recurrenceLimit = it.toInt() },
                                    valueRange = 2f..60f,
                                    steps = 58,
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
                Spacer(modifier = Modifier.height(12.dp))
            }

            AnimatedVisibility(visible = isCC) {
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
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                        // Selettore Carta di Credito (Se ce ne sono)
                        if (availableCreditCards.isNotEmpty()) {
                            OutlinedTextField(
                                value = availableCreditCards.find { it.id == creditCardId }?.name ?: stringResource(R.string.select_credit_card),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.credit_card)) },
                                trailingIcon = {
                                    IconButton(onClick = { showCreditCardDialog = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { showCreditCardDialog = true }.padding(12.dp),
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (!isEditing) {
                                        Modifier.clickable {
                                            isCreditCard = !isCreditCard
                                        }
                                    } else {
                                        Modifier
                                    },
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = isCreditCard,
                                onCheckedChange = { if (!isEditing) isCreditCard = it },
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

                        AnimatedVisibility(visible = !isEditing && !isCreditCard || (isEditing && transactionToEdit?.totalInstallments != null && transactionToEdit.totalInstallments > 1 && !transactionToEdit.isCreditCard)) {
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
                                                Modifier.clickable {
                                                    isInstallment = !isInstallment
                                                }
                                            } else {
                                                Modifier
                                            },
                                        )
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = isInstallment,
                                        onCheckedChange = { isInstallment = it },
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

            AnimatedVisibility(visible = (isCreditCard || isInstallment) && (type == TransactionType.EXPENSE || type == TransactionType.INCOME)) {
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

                    if (isInstallment) {
                        if (!isEditing) {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                            ) {
                                SegmentedButton(
                                    selected = calculationMode == "installments",
                                    onClick = { calculationMode = "installments" },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                ) {
                                    Text(stringResource(R.string.calc_mode_installments))
                                }
                                SegmentedButton(
                                    selected = calculationMode == "amount",
                                    onClick = { calculationMode = "amount" },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                ) {
                                    Text(stringResource(R.string.calc_mode_amount))
                                }
                            }
                        }

                        if (calculationMode == "installments" || isEditing) {
                            Text(stringResource(R.string.number_of_installments, installmentsCount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Slider(
                                value = installmentsCount.toFloat(),
                                onValueChange = { installmentsCount = it.toInt() },
                                valueRange = 2f..12f,
                                steps = 10,
                                enabled = !isEditing,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            if (amount > 0 && installmentsCount > 0) {
                                val amountPerInstallment = amount / installmentsCount
                                Text(
                                    text = stringResource(R.string.calc_amount_per_installment, String.format(locale, "%.2f %s", amountPerInstallment, currencySymbol)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = installmentAmountText,
                                onValueChange = { installmentAmountText = it.replace(',', '.') },
                                label = { Text(stringResource(R.string.installment_amount_label)) },
                                placeholder = { Text("0.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            )

                            val totalAmount = amountText.toDoubleOrNull() ?: 0.0
                            val installmentAmount = installmentAmountText.toDoubleOrNull() ?: 0.0
                            if (totalAmount > 0 && installmentAmount > 0) {
                                val calculatedInstallments = ceil(totalAmount / installmentAmount).toInt()
                                installmentsCount = calculatedInstallments
                                Text(
                                    text = stringResource(R.string.number_of_installments, calculatedInstallments),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (!isEditing || (isEditing && isCreditCard)) {
                            AnimatedVisibility(visible = isCreditCard) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                applyCcDelayToInstallments =
                                                    !applyCcDelayToInstallments
                                            }
                                            .padding(vertical = 8.dp),
                                    ) {
                                        Checkbox(
                                            checked = applyCcDelayToInstallments,
                                            onCheckedChange = { isChecked ->
                                                applyCcDelayToInstallments = isChecked
                                                installmentStartDateStr = if (isChecked) {
                                                    try {
                                                        val tDate = LocalDate.parse(dateStr, displayFormatter)
                                                        tDate.plusMonths(1).withDayOfMonth(15).format(displayFormatter)
                                                    } catch (e: Exception) {
                                                        dateStr
                                                    }
                                                } else {
                                                    dateStr
                                                }
                                            },
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = stringResource(R.string.apply_cc_delay),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                text = if (applyCcDelayToInstallments) {
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

                        if (!isEditing || (isEditing && !isCreditCard && isInstallment)) {
                            OutlinedTextField(
                                value = installmentStartDateStr,
                                onValueChange = { installmentStartDateStr = it },
                                label = { Text(stringResource(R.string.first_installment_date)) },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { showInstallmentDatePicker = true }) {
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
                    } else if (isCreditCard && !isInstallment) {
                        val effectiveDate = try {
                            if (transactionToEdit != null && transactionToEdit.effectiveDate.isNotEmpty()) {
                                transactionToEdit.effectiveDate
                            } else {
                                val tDate = LocalDate.parse(dateStr, displayFormatter)
                                if (selectedCard != null) {
                                    DateUtils.calculateEffectiveDate(tDate, selectedCard)
                                } else {
                                    // Fallback per la vecchia logica
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
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
    }

    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text(stringResource(R.string.original_currency_dialog_title)) },
            text = {
                Column {
                    listOf(currencySymbol, "USD", "EUR", "GBP", "JPY", "CHF", "HUF").forEach { symbol ->
                        Text(
                            text = symbol,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    originalCurrency = symbol
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCurrencyDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    // Dialog Selezione Carta
    if (showCreditCardDialog && availableCreditCards.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showCreditCardDialog = false },
            title = { Text(stringResource(R.string.select_credit_card)) },
            text = {
                Column {
                    availableCreditCards.forEach { card ->
                        Text(
                            text = card.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    creditCardId = card.id
                                    isInstallment = card.type == CardType.REVOLVING
                                    showCreditCardDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCreditCardDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (showRecurrenceTypeDialog) {
        AlertDialog(
            onDismissRequest = { showRecurrenceTypeDialog = false },
            title = { Text(stringResource(R.string.recurrence_label)) },
            text = {
                Column {
                    RecurrenceType.entries.forEach { typeEntry ->
                        Text(
                            text = when (typeEntry) {
                                RecurrenceType.NONE -> stringResource(R.string.recurrence_none)
                                RecurrenceType.DAILY -> stringResource(R.string.recurrence_daily)
                                RecurrenceType.WEEKLY -> stringResource(R.string.recurrence_weekly)
                                RecurrenceType.MONTHLY -> stringResource(R.string.recurrence_monthly)
                                RecurrenceType.YEARLY -> stringResource(R.string.recurrence_yearly)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    recurrenceType = typeEntry
                                    showRecurrenceTypeDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showRecurrenceTypeDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (showPreviousMonthAlert) {
        AlertDialog(
            onDismissRequest = { showPreviousMonthAlert = false },
            title = { Text(stringResource(R.string.warning_past_date_title)) },
            text = { Text(stringResource(R.string.warning_past_date_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPreviousMonthAlert = false
                        ignoreDateWarning = true
                        trySave()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.proceed_and_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreviousMonthAlert = false }) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            },
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                LocalDate.parse(dateStr, displayFormatter).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (e: Exception) {
                Instant.now().toEpochMilli()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        dateStr = selectedDate.format(displayFormatter)

                        if (applyCcDelayToInstallments && isCreditCard && isInstallment) {
                            installmentStartDateStr = selectedDate.plusMonths(1).withDayOfMonth(15).format(displayFormatter)
                        } else if (!isCreditCard && isInstallment) {
                            installmentStartDateStr = dateStr
                        }
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showInstallmentDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                LocalDate.parse(installmentStartDateStr, displayFormatter).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (e: Exception) {
                Instant.now().toEpochMilli()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showInstallmentDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        installmentStartDateStr = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(displayFormatter)
                    }
                    showInstallmentDatePicker = false
                }) { Text("OK") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
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
                                showDeleteDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(stringResource(R.string.delete_this_and_subsequent))
                        }
                        TextButton(
                            onClick = {
                                onDelete(transactionToEdit.id, DeleteType.SINGLE)
                                showDeleteDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(stringResource(R.string.delete_single_installment))
                        }
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text(stringResource(R.string.cancel).uppercase())
                        }
                    }
                } else {
                    TextButton(
                        onClick = {
                            transactionToEdit?.let { onDelete(it.id, DeleteType.SINGLE) }
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(R.string.delete_uppercase))
                    }
                }
            },
            dismissButton = {
                if (transactionToEdit?.groupId == null || ((transactionToEdit.totalInstallments ?: 0) <= 1 && transactionToEdit.recurrenceType == RecurrenceType.NONE)) {
                    TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
    }
}

@Composable
fun CategorySelector(
    type: TransactionType,
    selectedCategoryId: String?,
    onCategorySelected: (String) -> Unit,
    availableCategories: List<CategoryEntity>,
    viewModel: ExpenseViewModel?,
) {
    val frequentCategories by if (viewModel != null) {
        viewModel.getFrequentCategories(type).collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf(emptyList<CategoryEntity>()) }
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
        // Search Bar
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

        // Frequent Categories Section
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

        // All Categories Grid
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
            .height(84.dp) // Fixed height to prevent flickering during layout passes
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
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
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
