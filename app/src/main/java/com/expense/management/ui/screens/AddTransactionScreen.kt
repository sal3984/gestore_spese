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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    suggestions: List<String>,
    availableCategories: List<CategoryEntity>, // NUOVO: Lista dinamica delle categorie
    onSave: (TransactionEntity) -> Unit,
    onDelete: (String) -> Unit, // ID String (UUID)
    onBack: () -> Unit
) {
    // Stati Transazione
    var type by remember { mutableStateOf(transactionToEdit?.type ?: "expense") }
    var amountText by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(transactionToEdit?.description ?: "") }
    var isDescriptionExpanded by remember { mutableStateOf(false) }


    // Filtra le categorie in base al tipo selezionato
    val currentTypeCategories = remember(availableCategories, type) {
        availableCategories.filter { it.type == type }
    }

    // Seleziona categoria: usa quella esistente se c'è, altrimenti la prima disponibile del tipo corrente, o una di fallback
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
    // Logica mutualmente esclusiva: se isInstallment è true, ccDelay standard è ignorato in favore del calcolo rateale
    var isInstallment by remember { mutableStateOf(false) }
    var installmentsCount by remember { mutableStateOf(3) } // Default 3 rate

    var ignoreDateWarning by remember { mutableStateOf(false) }

    // Stati UI
    var dateStr by remember {
        mutableStateOf(
            transactionToEdit?.date ?: LocalDate.now().format(DateTimeFormatter.ofPattern(dateFormat))
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val displayFormatter = remember(dateFormat) { DateTimeFormatter.ofPattern(dateFormat) }
    val context = LocalContext.current

    var showPreviousMonthAlert by remember { mutableStateOf(false) }

    // Messaggi localizzati
    val errorInvalidInput = stringResource(R.string.error_invalid_input)
    val errorInvalidDateFormat = stringResource(R.string.error_invalid_date_format)
    val errorPastLimitDate = stringResource(R.string.error_past_limit_date)


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

        // GESTIONE ID ROBUSTA: Se è una modifica, usa l'ID esistente. Se è nuova, genera un nuovo UUID.
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
        // Se è rateale e NON è una modifica (solo nuove transazioni rateali per ora per semplicità)
        if (isCreditCard && isInstallment && transactionToEdit == null) {
            val installmentAmount = amount / installmentsCount
            val groupId = UUID.randomUUID().toString()

            // Loop per creare N transazioni
            for (i in 0 until installmentsCount) {
                // Calcola la data della rata: aggiungi 'i' mesi alla data originale
                val installmentDate = transactionDate.plusMonths(i.toLong())

                // Calcola l'effective date per questa specifica rata
                // NOTA: Le rate seguono la logica standard del ritardo CC se necessario, ma di base sono mensili
                val effectiveDate = calculateEffectiveDate(installmentDate, true, ccDelay)

                val newId = if(i==0) transactionId else UUID.randomUUID().toString() // La prima usa l'ID generato sopra

                onSave(
                    TransactionEntity(
                        id = newId,
                        date = installmentDate.format(displayFormatter),
                        description = "$description (Rata ${i+1}/$installmentsCount)", // Descrizione con info rata
                        amount = installmentAmount,
                        categoryId = selectedCategory,
                        type = type,
                        isCreditCard = true,
                        originalAmount = originalAmount / installmentsCount,
                        originalCurrency = originalCurrency,
                        effectiveDate = effectiveDate,
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
                    installmentNumber = transactionToEdit?.installmentNumber, // Mantieni se esiste (modifica rata)
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
            // Sostituisci BottomAppBar con Surface per avere il pieno controllo del layout
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp, // Dà l'effetto di "rialzo" tipico della barra inferiore
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp) // Togli il bottom qui
                        .navigationBarsPadding() // Aggiungi questo: spinge su il contenuto sopra la barra di sistema
                        .padding(bottom = 16.dp) // Un po' di margine extra estetico
                    // Opzionale: se usi edge-to-edge, aggiungi .navigationBarsPadding() qui
                ) {
                    Button(
                        onClick = { trySave() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp), // Altezza esplicita per evitare che sia troppo basso
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp) // Opzionale: arrotonda un po' il pulsante
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

            // Toggle Tipo (Spesa/Entrata)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("expense", "income").forEach { itemType ->
                    val isSelected = type == itemType
                    val color = if (itemType == "expense") Color(0xFFEF5350) else Color(0xFF43A047)
                    Text(
                        text = if (itemType == "expense") stringResource(R.string.expense_type) else stringResource(R.string.income_type),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                type = itemType
                                // Cambia categoria di default se cambia il tipo
                                // AGGIORNAMENTO: Quando cambia il tipo, resetta selectedCategory
                                // alla prima categoria valida del nuovo tipo
                                val newCategory =
                                    availableCategories.filter { it.type == itemType }.firstOrNull()
                                if (newCategory != null) {
                                    selectedCategory = newCategory.id
                                }
                            }
                            .background(if (isSelected) color else Color.Transparent)
                            .padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- ORDINE CAMBIATO: Data -> Descrizione -> Importo ---

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

            // 2. Campo Descrizione con Autocomplete
            val filteredSuggestions = remember(description, suggestions) {
                if (description.isBlank()) {
                    emptyList()
                } else {
                    suggestions.filter {
                        it.contains(description, ignoreCase = true) &&
                            !it.equals(description, ignoreCase = true)
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = isDescriptionExpanded,
                onExpandedChange = { isDescriptionExpanded = !isDescriptionExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        isDescriptionExpanded = true
                    },
                    label = { Text(stringResource(R.string.description)) },
                    placeholder = { stringResource(R.string.description_placeholder)},
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDescriptionExpanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Sentences
                    )
                )

                if (filteredSuggestions.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = isDescriptionExpanded,
                        onDismissRequest = { isDescriptionExpanded = false }
                    ) {
                        filteredSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(text = suggestion) },
                                onClick = {
                                    description = suggestion
                                    isDescriptionExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Campo Importo nella Valuta Principale
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.replace(',', '.') },
                label = { Text(stringResource(R.string.amount_converted_label, currencySymbol)) },
                placeholder = { Text("100.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // NUOVO: Opzione Valuta Originale
            AnimatedVisibility(visible = originalCurrency != currencySymbol) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Importo Originale
                        OutlinedTextField(
                            value = originalAmountText,
                            onValueChange = { originalAmountText = it.replace(',', '.') },
                            label = { Text(stringResource(R.string.amount_original_label)) },
                            placeholder = { Text("50.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Pulsante Valuta Originale
                        OutlinedTextField(
                            value = originalCurrency,
                            onValueChange = { originalCurrency = it.uppercase(Locale.ROOT) },
                            label = { Text(stringResource(R.string.currency_original_label)) },
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
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

            // Selezione Categoria
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
                                Text(
                                    text = category.icon,
                                    fontSize = 24.sp
                                )
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isCreditCard = !isCreditCard }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = isCreditCard, onCheckedChange = { isCreditCard = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.credit_card_payment), style = MaterialTheme.typography.bodyLarge)
            }

            // Opzione Rateale (Visibile solo se CC è attivo e non stiamo modificando una transazione esistente)
            // AGGIORNATO: Il titolo ora è "Pagamento Rateale (Alternativo al saldo mensile)" per chiarezza
            AnimatedVisibility(visible = isCreditCard && transactionToEdit == null) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 8.dp, top = 8.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isInstallment, onCheckedChange = { isInstallment = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Pagamento Rateale", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Paga in più rate mensili invece che in un'unica soluzione posticipata.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if(isInstallment) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) // Usato HorizontalDivider invece di Divider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Numero Rate: $installmentsCount", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = installmentsCount.toFloat(),
                            onValueChange = { installmentsCount = it.toInt() },
                            valueRange = 2f..12f, // Da 2 a 12 rate
                            steps = 10
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
                        trySave() // Richiama salvataggio dopo conferma
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

    // Dialog Data Picker
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
                    }
                    showDatePicker = false
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
            text = { Text(stringResource(R.string.delete_transaction_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionToEdit?.let { onDelete(it.id) }
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
        // Logica per CC: Pagamento rateizzato ogni [ccDelay] del mese successivo
        var nextPaymentMonth = YearMonth.from(transactionDate).plusMonths(1)
        var paymentDate = nextPaymentMonth.atDay(ccDelay.coerceIn(1, nextPaymentMonth.lengthOfMonth()))

        // Se la data di pagamento è già passata nel mese successivo (es. oggi è 20/01 e paymentDate è 15/02, OK; ma se transactionDate è 20/02 e pagamento è 15/02, si sposta al mese dopo)
        if (paymentDate.isBefore(transactionDate)) {
            nextPaymentMonth = nextPaymentMonth.plusMonths(1)
            paymentDate = nextPaymentMonth.atDay(ccDelay.coerceIn(1, nextPaymentMonth.lengthOfMonth()))
        }
        paymentDate

    } else {
        // Per tutte le altre: la data della transazione stessa
        transactionDate
    }
    return effectiveDate.format(DateTimeFormatter.ISO_LOCAL_DATE) // Salva in formato standard per l'ordinamento
}
