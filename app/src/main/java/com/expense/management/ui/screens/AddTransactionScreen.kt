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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expense.management.R
import com.expense.management.data.CategoryEntity
import com.expense.management.data.TransactionEntity
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.UUID

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
    onDelete: (String) -> Unit, // ID String (UUID)
    onBack: () -> Unit,
    onDescriptionChange: (String) -> Unit
) {
    val displayFormatter = remember(dateFormat) { DateTimeFormatter.ofPattern(dateFormat) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Stati Transazione
    var type by remember { mutableStateOf(transactionToEdit?.type ?: "expense") }
    var amountText by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(transactionToEdit?.description ?: "") }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    // Filtra le categorie in base al tipo selezionato
    val currentTypeCategories = remember(availableCategories, type) {
        availableCategories.filter { it.type == type }
    }

    // Seleziona categoria
    var selectedCategory by remember(type, currentTypeCategories) {
        mutableStateOf(
            transactionToEdit?.categoryId.takeIf { id -> availableCategories.any { it.id == id } } // Mantieni se esiste
                ?: transactionToEdit?.categoryId.takeIf { transactionToEdit != null } // Mantieni anche se non esiste più (caso limite)
                ?: currentTypeCategories.firstOrNull()?.id // Altrimenti prendi la prima del tipo
                ?: if (type == "expense") "food" else "salary" // Fallback estremo
        )
    }

    var isCreditCard by remember { mutableStateOf(transactionToEdit?.isCreditCard ?: false) }

    // Stati per Valuta
    var originalAmountText by remember { mutableStateOf(transactionToEdit?.originalAmount?.toString() ?: "") }
    var originalCurrency by remember { mutableStateOf(transactionToEdit?.originalCurrency ?: currencySymbol) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    // Stati Pagamento Rateale
    // Se è una nuova transazione, usiamo l'impostazione globale ccPaymentMode
    // Se stiamo modificando, usiamo lo stato salvato nella transazione (se disponibile) o l'impostazione globale
    var isInstallment by remember {
        mutableStateOf(
            if (transactionToEdit != null) {
                // Se è una rata o ha totalInstallments > 1, è rateale
                (transactionToEdit.totalInstallments ?: 1) > 1
            } else {
                // Nuova transazione: Default dalle impostazioni
                ccPaymentMode == "installment"
            }
        )
    }

    // Se cambiamo ccPaymentMode dall'esterno (settings), vogliamo aggiornare isInstallment per le nuove transazioni?
    // Probabilmente sì, ma solo se non è in modifica.
    LaunchedEffect(ccPaymentMode) {
        if (transactionToEdit == null) {
            isInstallment = (ccPaymentMode == "installment")
        }
    }


    var installmentsCount by remember {
        mutableIntStateOf(transactionToEdit?.totalInstallments ?: 3)
    } // Default 3 rate o valore esistente

    var applyCcDelayToInstallments by remember { mutableStateOf(true) }


    var ignoreDateWarning by remember { mutableStateOf(false) }

    // Stati UI
    var dateStr by remember {
        mutableStateOf(
            transactionToEdit?.date ?: LocalDate.now().format(displayFormatter)
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // --- NUOVO: Stato per la data di inizio rata (solo per modalità rateale) ---
    // Inizializzazione corretta: Se è una nuova transazione e applyCcDelayToInstallments è true, calcoliamo il 15 del mese successivo
    var installmentStartDateStr by remember {
        mutableStateOf(
            if (transactionToEdit == null && applyCcDelayToInstallments) {
                try {
                    val tDate = LocalDate.now()
                    tDate.plusMonths(1).withDayOfMonth(15).format(displayFormatter)
                } catch (e: Exception) {
                    LocalDate.now().format(displayFormatter)
                }
            } else {
                 // Default o manteniamo quello che c'era (in realtà per edit non usiamo questa logica solitamente)
                 LocalDate.now().format(displayFormatter)
            }
        )
    }
    var showInstallmentDatePicker by remember { mutableStateOf(false) }


    var showPreviousMonthAlert by remember { mutableStateOf(false) }



    // Messaggi localizzati
    val errorInvalidInput = stringResource(R.string.error_invalid_input)
    val errorInvalidDateFormat = stringResource(R.string.error_invalid_date_format)
    val errorPastLimitDate = stringResource(R.string.error_past_limit_date)
    val installmentLabel = stringResource(R.string.installment)


    // Funzione di Salvataggio
    fun trySave() {
        val amount = try { amountText.replace(',', '.').toDouble() } catch (e: Exception) { 0.0 }
        val originalAmount = try { originalAmountText.replace(',', '.').toDouble() } catch (e: Exception) { amount }

        if (amount <= 0 || description.isBlank()) {
            scope.launch {
                snackbarHostState.showSnackbar(errorInvalidInput, "OK")
            }
            return
        }

        val transactionDate: LocalDate = try {
            LocalDate.parse(dateStr, displayFormatter)
        } catch (e: DateTimeParseException) {
            scope.launch { snackbarHostState.showSnackbar(errorInvalidDateFormat, "OK") }
            return
        }

        // GESTIONE ID ROBUSTA
        val transactionId = transactionToEdit?.id ?: UUID.randomUUID().toString()

        // LOGICA AVVISO MESE PRECEDENTE
        val limitMonth = YearMonth.now().minusMonths(1)
        val transactionMonth = YearMonth.from(transactionDate)

        if (transactionMonth.isBefore(limitMonth)) {
            scope.launch {
                val formattedMonth = limitMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
                val message = String.format(errorPastLimitDate, formattedMonth)
                snackbarHostState.showSnackbar(message, "OK")
            }
            return
        }

        if (transactionMonth.isBefore(YearMonth.now()) && transactionToEdit == null && !ignoreDateWarning) {
            showPreviousMonthAlert = true
            return
        }

        // LOGICA RATEALE
        if (isCreditCard && isInstallment && transactionToEdit == null) {
            val installmentAmount = amount / installmentsCount
            val groupId = UUID.randomUUID().toString()

            // Parsing della data di inizio rata scelta dall'utente
            val startInstallmentDate: LocalDate = try {
                 LocalDate.parse(installmentStartDateStr, displayFormatter)
            } catch (e: DateTimeParseException) {
                 // Fallback alla data transazione se c'è un errore, ma non dovrebbe capitare col picker
                 transactionDate
            }

            // Loop per creare N transazioni
            for (i in 0 until installmentsCount) {
                // La data "contabile" della rata è la data di partenza + i mesi
                val installmentDate = startInstallmentDate.plusMonths(i.toLong())

                val shouldUseCcDelay = applyCcDelayToInstallments

                // Calcola l'effective date per questa specifica rata
                // Se è rateale, usiamo la data calcolata come effective date.
                // Nota: Se l'utente ha scelto una data specifica per la rata, quella dovrebbe essere rispettata.
                // Qui assumiamo che la data scelta sia già quella "di addebito" desiderata o comunque la data base.
                // Se vogliamo applicare comunque il ritardo CC sulla data scelta, usiamo calculateEffectiveDate.
                // Se l'utente ha scelto esplicitamente la data di partenza della rata, probabilmente intende quella.
                // Tuttavia, per coerenza col sistema CC, applichiamo il ritardo se la data scelta non è già "futura" abbastanza?
                // Semplificazione: Usiamo calculateEffectiveDate sulla data della rata, che rispetterà la logica del ritardo CC se configurata.

                val effectiveDate = if (applyCcDelayToInstallments) {
                    // CASO 1: Applica ritardo -> 15 del mese successivo
                    installmentDate // Va al mese successivo
                        .withDayOfMonth(15)  // Imposta il giorno 15 (o usa 'ccDelay' se preferisci variabile)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE)
                } else {
                    // CASO 2: Niente ritardo -> Mantieni la data della rata così com'è
                    installmentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                }

                val newId = if(i==0) transactionId else UUID.randomUUID().toString()

                onSave(
                    TransactionEntity(
                        id = newId,
                        date = installmentDate.format(displayFormatter), // Data visualizzata della rata
                        description = "$description ($installmentLabel ${i+1}/$installmentsCount)",
                        amount = installmentAmount,
                        categoryId = selectedCategory,
                        type = type,
                        isCreditCard = true,
                        originalAmount = originalAmount / installmentsCount,
                        originalCurrency = originalCurrency,
                        effectiveDate = effectiveDate, // Data effettiva per il saldo
                        installmentNumber = i + 1,
                        totalInstallments = installmentsCount,
                        groupId = groupId
                    )
                )
            }
        } else {
            // Logica Standard (Singola transazione)
            onSave(
                TransactionEntity(
                    id = transactionId,
                    date = transactionDate.format(displayFormatter),
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
                    groupId = transactionToEdit?.groupId
                )
            )
        }
        onBack()
    }

    // UI Principale
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (transactionToEdit == null) stringResource(R.string.add_transaction) else stringResource(R.string.edit_transaction)) },
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
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp)
                ) {
                    Button(
                        onClick = { trySave() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (transactionToEdit == null)
                                stringResource(R.string.save_transaction)
                            else
                                stringResource(R.string.update_transaction),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Toggle Tipo (Spesa/Entrata) - SegmentedButton
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                val expenseSelected = type == "expense"
                SegmentedButton(
                    selected = expenseSelected,
                    onClick = {
                        if (type != "expense") {
                            type = "expense"
                            val newCategory = availableCategories.firstOrNull { it.type == "expense" }
                            if (newCategory != null) selectedCategory = newCategory.id
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Color(0xFFEF5350),
                        activeContentColor = Color.White,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    )
                ) {
                    Text(stringResource(R.string.expense_type))
                }

                val incomeSelected = type == "income"
                SegmentedButton(
                    selected = incomeSelected,
                    onClick = {
                        if (type != "income") {
                            type = "income"
                            val newCategory = availableCategories.firstOrNull { it.type == "income" }
                            if (newCategory != null) selectedCategory = newCategory.id
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Color(0xFF43A047),
                        activeContentColor = Color.White,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    )
                ) {
                    Text(stringResource(R.string.income_type))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Data Transazione
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
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Descrizione con Autocomplete
            ExposedDropdownMenuBox(
                expanded = isDescriptionExpanded && suggestions.isNotEmpty(),
                onExpandedChange = { isDescriptionExpanded = it }
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
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = isDescriptionExpanded && suggestions.isNotEmpty(),
                    onDismissRequest = { isDescriptionExpanded = false }
                ) {
                    suggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(text = suggestion) },
                            onClick = {
                                description = suggestion
                                isDescriptionExpanded = false
                                onDescriptionChange("")
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Importo
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.replace(',', '.') },
                label = { Text(stringResource(R.string.amount_converted_label, currencySymbol)) },
                placeholder = { Text("100.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Valuta Originale
            AnimatedVisibility(visible = originalCurrency != currencySymbol) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = originalAmountText,
                            onValueChange = { originalAmountText = it.replace(',', '.') },
                            label = { Text(stringResource(R.string.amount_original_label)) },
                            placeholder = { Text("50.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
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
                                .clickable { showCurrencyDialog = true }
                        )
                    }
                    Text(
                        text = stringResource(R.string.main_currency_hint, currencySymbol),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                }
            }

            if (originalCurrency == currencySymbol) {
                TextButton(
                    onClick = { showCurrencyDialog = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.set_original_currency))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Categoria
            Text(
                stringResource(R.string.category_label),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                if (currentTypeCategories.isEmpty()) {
                    Text(stringResource(R.string.no_categories_error), modifier = Modifier.padding(8.dp))
                } else {
                    currentTypeCategories.forEach { category ->
                        val isSelected = selectedCategory == category.id
                        val color = if (type == "expense") Color(0xFFEF5350) else Color(0xFF43A047)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clickable { selectedCategory = category.id }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) color else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = category.icon, fontSize = 24.sp)
                            }
                            Text(
                                text = category.label.split(" ").first(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Opzione Carta di Credito
            // Modifica: disabilitata se stiamo modificando una transazione esistente
            val isEditing = transactionToEdit != null
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if(!isEditing) Modifier.clickable { isCreditCard = !isCreditCard } else Modifier)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isCreditCard,
                    onCheckedChange = { if(!isEditing) isCreditCard = it },
                    enabled = !isEditing // Disabilita la checkbox in modifica
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.credit_card_payment), style = MaterialTheme.typography.bodyLarge)
            }

            // Gestione Avanzata Carta di Credito (Mutuamente esclusiva + Data personalizzabile)
            // VISIBILE SE: (isCreditCard è true) E (Siamo in modalità creazione OPPURE siamo in modifica di una transazione esistente)
            AnimatedVisibility(visible = isCreditCard) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 8.dp, top = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    .padding(16.dp) // Padding interno aumentato
                ) {
                    Text(
                        text = stringResource(R.string.payment_options),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // SE STIAMO MODIFICANDO (transactionToEdit != null), BLOCCA LE OPZIONI
                    val isSelectionLocked = (ccPaymentMode != "manual") || isEditing

                    // Segmented Button per scelta Saldo vs Rateale
                    // Ora riflette ccPaymentMode, ma l'utente può sovrascrivere per questa transazione
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !isInstallment,
                            onClick = { if (!isSelectionLocked) isInstallment = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            enabled = !isSelectionLocked
                        ) {
                            Text(stringResource(R.string.single_balance))
                        }
                        SegmentedButton(
                            selected = isInstallment,
                            onClick = { if (!isSelectionLocked) isInstallment = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            enabled = !isSelectionLocked
                        ) {
                            Text(stringResource(R.string.installment_plan))
                        }
                    }

                    if (isSelectionLocked) {
                        Text(
                            text = if (isEditing) stringResource(R.string.mode_locked_by_settings) else stringResource(R.string.mode_locked_by_settings),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if(isCreditCard && isInstallment) {
                        // SEZIONE RATEALE
                        Text(stringResource(R.string.number_of_installments, installmentsCount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Slider(
                            value = installmentsCount.toFloat(),
                            onValueChange = { installmentsCount = it.toInt() },
                            valueRange = 2f..12f,
                            steps = 10,
                            enabled = !isEditing // Disabilita se in modifica
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // SE IN MODIFICA, NASCONDI LE OPZIONI DI CREAZIONE PIANO (Delay, Data Inizio)
                        if (!isEditing) {

                            // NUOVO SWITCH: Applica ritardo CC
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { applyCcDelayToInstallments = !applyCcDelayToInstallments }
                                    .padding(vertical = 8.dp)
                            ) {
                                Checkbox(
                                    checked = applyCcDelayToInstallments,
                                    onCheckedChange = { isChecked ->
                                        applyCcDelayToInstallments = isChecked
                                        // LOGICA AGGIUNTA: Se attivo il ritardo, imposto il 15 del mese successivo
                                        if (isChecked) {
                                            try {
                                                val tDate = LocalDate.parse(dateStr, displayFormatter)
                                                val nextMonth15 = tDate.plusMonths(1).withDayOfMonth(15)
                                                installmentStartDateStr = nextMonth15.format(displayFormatter)
                                            } catch (e: Exception) {
                                                // Fallback in caso di errore parsing
                                                installmentStartDateStr = dateStr
                                            }
                                        } else {
                                            // Se lo disattivo, torno alla data della transazione
                                            installmentStartDateStr = dateStr
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.apply_cc_delay),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (applyCcDelayToInstallments)
                                            stringResource(R.string.cc_delay_installment_message_on)
                                        else
                                            stringResource(R.string.cc_delay_installment_message_off),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Data Partenza Prima Rata
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
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                stringResource(R.string.first_installment_date_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                    } else {
                        // SEZIONE SALDO UNICO
                        // Mostriamo la data di addebito prevista (calcolata)
                        val effectiveDate = try {
                            // Se stiamo modificando, usiamo la effectiveDate salvata se disponibile, altrimenti ricalcoliamo
                            if (transactionToEdit != null && transactionToEdit.effectiveDate.isNotEmpty()) {
                                transactionToEdit.effectiveDate
                            } else {
                                // Parsing della data transazione corrente
                                val tDate = LocalDate.parse(dateStr, displayFormatter)
                                calculateEffectiveDate(tDate, true, ccDelay)
                            }
                        } catch (e: Exception) { "" }

                        val formattedEffectiveDate = try {
                            LocalDate.parse(effectiveDate, DateTimeFormatter.ISO_LOCAL_DATE).format(displayFormatter)
                        } catch (e: Exception) { effectiveDate }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                             Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                             Spacer(modifier = Modifier.width(8.dp))
                             Column {
                                 Text(stringResource(R.string.expected_debit_date), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                 Text(formattedEffectiveDate, style = MaterialTheme.typography.bodyLarge)
                             }
                        }
                        Text(
                            stringResource(R.string.expected_debit_date_calc, ccDelay),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // Dialog Selezione Valuta
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text(stringResource(R.string.original_currency_dialog_title)) },
            text = {
                Column {
                    val commonCurrencies = listOf(currencySymbol, "USD", "EUR", "GBP", "JPY", "CHF")
                    commonCurrencies.forEach { symbol ->
                        Text(
                            text = symbol,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    originalCurrency = symbol
                                    showCurrencyDialog = false
                                }
                                .padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCurrencyDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    // Dialog Avviso Mese Precedente
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
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.proceed_and_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreviousMonthAlert = false }) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            }
        )
    }

    // Dialog Data Picker Transazione
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                LocalDate.parse(dateStr, displayFormatter).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (e: Exception) {
                Instant.now().toEpochMilli()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        dateStr = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(displayFormatter)

                        // LOGICA MODIFICATA:
                        // Se cambio la data transazione, aggiorno la data inizio rata rispettando la scelta del ritardo
                        if (applyCcDelayToInstallments) {
                            try {
                                val tDate = LocalDate.parse(dateStr, displayFormatter)
                                val nextMonth15 = tDate.plusMonths(1).withDayOfMonth(15)
                                installmentStartDateStr = nextMonth15.format(displayFormatter)
                            } catch (e: Exception) {
                                installmentStartDateStr = dateStr
                            }
                        } else {
                            installmentStartDateStr = dateStr
                        }
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // NUOVO: Dialog Data Picker per Rata
    if (showInstallmentDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                LocalDate.parse(installmentStartDateStr, displayFormatter).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (e: Exception) {
                Instant.now().toEpochMilli()
            }
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
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Dialog Conferma Cancellazione
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_transaction_title)) },
            text = {
                // Se è una rata (ha un groupId), avvisa che verranno cancellate tutte
                if (transactionToEdit?.groupId != null) {
                     Text(stringResource(R.string.delete_installment_message))
                } else {
                     Text(stringResource(R.string.delete_transaction_message))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionToEdit?.let { onDelete(it.id) } // onDelete gestirà la logica del groupId
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_uppercase))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            }
        )
    }
}

// Funzione helper per il calcolo della data di addebito
private fun calculateEffectiveDate(transactionDate: LocalDate, isCreditCard: Boolean, ccDelay: Int): String {
    val effectiveDate = if (isCreditCard) {
        // 1. Calcola il mese di pagamento previsto (Mese successivo)
        var paymentMonth = YearMonth.from(transactionDate).plusMonths(1)

        // 2. Calcola la data, usando ccDelay come numero di MESI di ritardo
        // Se ccDelay è 0 (Immediato) -> transactionDate
        // Se ccDelay è > 0 -> Giorno 15 del mese (paymentMonth + (ccDelay-1))

        if (ccDelay == 0) {
            transactionDate
        } else {
            // Se c'è ritardo, fissiamo al giorno 15 del mese successivo (+eventuali mesi extra se ccDelay > 1)
            // Nota: ccDelay=1 significa "1 Mese dopo".
            // La logica precedente usava ccDelay come GIORNO nel coerceIn. Correggiamo.

            // Se ccDelay rappresenta i mesi di ritardo (es. 1), allora:
            // Mese base: transactionDate
            // Mese target: transactionDate + ccDelay mesi
            // Giorno target: 15 (standard)

            val targetMonth = YearMonth.from(transactionDate).plusMonths(ccDelay.toLong())
            targetMonth.atDay(15)
        }

    } else {
        transactionDate
    }
    return effectiveDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
}
