package com.expense.management.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expense.management.R
import com.expense.management.data.CategoryEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    transactionToEdit: TransactionEntity?,
    currencySymbol: String,
    dateFormat: String,
    ccDelay: Int,
    ccPaymentMode: String,
    suggestions: List<String>,
    availableCategories: List<CategoryEntity>,
    onSave: (TransactionEntity) -> Unit,
    onDelete: (String, DeleteType) -> Unit,
    onBack: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onConvertAmount: suspend (String, String, Double) -> Double? = { _, _, _ -> null },
) {
    val displayFormatter = remember(dateFormat) { DateTimeFormatter.ofPattern(dateFormat) }
    val context = LocalContext.current
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

    var isCreditCard by remember { mutableStateOf(transactionToEdit?.isCreditCard ?: false) }

    var originalAmountText by remember { mutableStateOf(transactionToEdit?.originalAmount?.toString() ?: "") }
    var originalCurrency by remember { mutableStateOf(transactionToEdit?.originalCurrency ?: currencySymbol) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var isConverting by remember { mutableStateOf(false) } // State for loading indicator

    var isInstallment by remember { mutableStateOf(false) }
    val isEditing = transactionToEdit != null

    // --- Installment States ---
    var calculationMode by remember { mutableStateOf("installments") } // "installments" or "amount"
    var installmentAmountText by remember { mutableStateOf("") }
    var installmentsCount by remember {
        mutableIntStateOf(transactionToEdit?.totalInstallments ?: 3)
    }

    LaunchedEffect(isEditing, transactionToEdit, isCreditCard, ccPaymentMode) {
        if (isEditing) {
            isInstallment = (transactionToEdit?.totalInstallments ?: 1) > 1
        } else {
            if (isCreditCard) {
                isInstallment = when (ccPaymentMode) {
                    "installment" -> true
                    else -> false
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
                val formattedMonth = limitMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
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
            val totalAmount = amount
            val totalOriginalAmount = originalAmount
            val installmentAmountFromField = installmentAmountText.toDoubleOrNull() ?: 0.0

            val finalInstallmentsCount = if (calculationMode == "amount" && installmentAmountFromField > 0) {
                ceil(totalAmount / installmentAmountFromField).toInt()
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
                val effectiveDate = if (isCreditCard && applyCcDelayToInstallments) {
                    calculateEffectiveDate(installmentDate, true, ccDelay)
                } else {
                    installmentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                }

                val newId = if (i == 0) transactionId else UUID.randomUUID().toString()
                val installmentDateToSave = installmentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                val currentInstallmentAmount: Double
                val currentOriginalInstallmentAmount: Double
                val originalRatio = if (totalAmount > 0) totalOriginalAmount / totalAmount else 1.0

                if (calculationMode == "amount" && installmentAmountFromField > 0) {
                    currentInstallmentAmount = if (i < finalInstallmentsCount - 1) {
                        installmentAmountFromField
                    } else {
                        totalAmount - (installmentAmountFromField * (finalInstallmentsCount - 1))
                    }
                    currentOriginalInstallmentAmount = currentInstallmentAmount * originalRatio
                } else {
                    val regularInstallment = floor((totalAmount / finalInstallmentsCount) * 100) / 100
                    currentInstallmentAmount = if (i < finalInstallmentsCount - 1) {
                        regularInstallment
                    } else {
                        totalAmount - (regularInstallment * (finalInstallmentsCount - 1))
                    }

                    val regularOriginalInstallment = floor((totalOriginalAmount / finalInstallmentsCount) * 100) / 100
                    currentOriginalInstallmentAmount = if (i < finalInstallmentsCount - 1) {
                        regularOriginalInstallment
                    } else {
                        totalOriginalAmount - (regularOriginalInstallment * (finalInstallmentsCount - 1))
                    }
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
                    ),
                )
            }
        } else {
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
                    effectiveDate = calculateEffectiveDate(transactionDate, isCreditCard, ccDelay),
                    installmentNumber = transactionToEdit?.installmentNumber,
                    totalInstallments = transactionToEdit?.totalInstallments,
                    groupId = transactionToEdit?.groupId,
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
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
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
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
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

            Text(
                stringResource(R.string.category_label),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (currentTypeCategories.isEmpty()) {
                    Text(stringResource(R.string.no_categories_error), modifier = Modifier.padding(8.dp))
                } else {
                    currentTypeCategories.forEach { category ->
                        val isSelected = selectedCategory == category.id
                        val color = if (type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selectedCategory = category.id },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) color else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = category.icon,
                                    fontSize = 28.sp,
                                )
                            }
                            Text(
                                text = category.label.split(" ").first(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = type == TransactionType.EXPENSE) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (!isEditing) Modifier.clickable { isCreditCard = !isCreditCard } else Modifier)
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
                                Text(stringResource(R.string.credit_card_payment), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(
                                    text = stringResource(R.string.credit_card_payment_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        AnimatedVisibility(visible = !isEditing && !isCreditCard || (isEditing && transactionToEdit?.totalInstallments != null && transactionToEdit.totalInstallments > 1 && !transactionToEdit.isCreditCard)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (!isEditing) Modifier.clickable { isInstallment = !isInstallment } else Modifier)
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

            AnimatedVisibility(visible = (isCreditCard || isInstallment) && type == TransactionType.EXPENSE) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
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
                                    text = stringResource(R.string.calc_amount_per_installment, String.format(Locale.getDefault(), "%.2f %s", amountPerInstallment, currencySymbol)),
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
                                            .clickable { applyCcDelayToInstallments = !applyCcDelayToInstallments }
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
                                calculateEffectiveDate(tDate, true, ccDelay)
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
                            stringResource(R.string.expected_debit_date_calc, ccDelay),
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
                    } else {
                        stringResource(R.string.delete_transaction_message)
                    },
                )
            },
            confirmButton = {
                if (transactionToEdit?.groupId != null && (transactionToEdit.totalInstallments ?: 0) > 1) {
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
                if (transactionToEdit?.groupId == null || (transactionToEdit.totalInstallments ?: 0) <= 1) {
                    TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
    }
}

private fun calculateEffectiveDate(transactionDate: LocalDate, isCreditCard: Boolean, ccDelay: Int): String {
    val effectiveDate = if (isCreditCard) {
        if (ccDelay == 0) {
            transactionDate
        } else {
            val targetMonth = YearMonth.from(transactionDate).plusMonths(ccDelay.toLong())
            targetMonth.atDay(15)
        }
    } else {
        transactionDate
    }
    return effectiveDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
}
