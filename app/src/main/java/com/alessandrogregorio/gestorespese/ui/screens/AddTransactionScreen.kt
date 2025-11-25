package com.alessandrogregorio.gestorespese.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.alessandrogregorio.gestorespese.data.TransactionEntity
import com.alessandrogregorio.gestorespese.ui.screens.category.CATEGORIES
import com.alessandrogregorio.gestorespese.ui.screens.category.Category
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// La funzione calculateEffectiveDate è stata spostata alla fine del file, fuori dalla classe.
fun calculateEffectiveDate(movementDate: LocalDate, isCreditCard: Boolean, ccDelay: Int): LocalDate {
    return if (isCreditCard) {
        // Aggiunge un mese per ogni 'ccDelay'
        movementDate.plusMonths(ccDelay.toLong())
    } else {
        movementDate
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    ccDelay: Int,
    currencySymbol: String,
    onGetSuggestions: suspend (String) -> List<String>,
    onSave: (TransactionEntity) -> Unit,
    onNavigateBack: () -> Unit,
    transactionToEdit: TransactionEntity?
) {
    // Formatter UI (dd/MM/yyyy)
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Funzione helper per parsare la stringa di data, gestendo gli errori (per i dati esistenti)
    fun safeParseDate(dateString: String): LocalDate {
        return try {
            // Tentativo di parsing con il formato UI
            LocalDate.parse(dateString, dateFormatter)
        } catch (e: DateTimeParseException) {
            println("Errore di Parsing data: ${e.message}. Usando data corrente.")
            // Tentativo di fallback al formato ISO 8601 (buona pratica DB)
            try {
                LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e2: DateTimeParseException) {
                LocalDate.now()
            }
        }
    }

    // --- STATI FORM ---
    var amountStr by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(transactionToEdit?.description ?: "") }
    var categoryId by remember { mutableStateOf(transactionToEdit?.categoryId ?: "altro") }
    var type by remember { mutableStateOf(transactionToEdit?.type ?: "expense") }
    var isCreditCard by remember { mutableStateOf(transactionToEdit?.isCreditCard ?: false) }

    // STATI DATA
    var date by remember { mutableStateOf(LocalDate.now()) }
    var effectiveDate by remember { mutableStateOf(calculateEffectiveDate(LocalDate.now(), isCreditCard, ccDelay)) }

    // --- STATI AUTOCOMPLETE ---
    var suggestions by remember { mutableStateOf(emptyList<String>()) }
    var showSuggestions by remember { mutableStateOf(false) }

    // --- EFFETTI LATERALI E INIZIALIZZAZIONE ---

    LaunchedEffect(transactionToEdit) {
        transactionToEdit?.let {
            amountStr = it.amount.toString()
            description = it.description
            categoryId = it.categoryId
            type = it.type
            isCreditCard = it.isCreditCard
            // Assicurati che le date vengano inizializzate correttamente
            date = safeParseDate(it.date)
            effectiveDate = safeParseDate(it.effectiveDate)
        }
    }

    LaunchedEffect(date, isCreditCard, ccDelay) {
        // Ricalcola la data effettiva ogni volta che Data, CC o Delay cambiano
        effectiveDate = calculateEffectiveDate(date, isCreditCard, ccDelay)
    }

    LaunchedEffect(description) {
        if (description.length > 1) {
            // Un piccolo ritardo per evitare chiamate API ad ogni digitazione
            kotlinx.coroutines.delay(300)
            suggestions = onGetSuggestions(description)
            showSuggestions = suggestions.isNotEmpty()
        } else {
            showSuggestions = false
        }
    }

    // --- FUNZIONI DI SUPPORTO ---

    fun validateAndSave() {
        val amount = amountStr.replace(",", ".").toDoubleOrNull()
        if (amount == null || amount <= 0) {
            // In una vera app, mostreresti un Toast o uno SnackBar
            println("Errore: Importo non valido.")
            return
        }

        val transaction = TransactionEntity(
            id = transactionToEdit?.id ?: 0L,
            // Salva la data nel formato desiderato, qui uso lo stesso formatter UI
            date = date.format(dateFormatter),
            effectiveDate = effectiveDate.format(dateFormatter),
            amount = amount,
            description = description.trim().ifEmpty { categoryId },
            categoryId = categoryId,
            type = type,
            isCreditCard = isCreditCard
        )
        onSave(transaction)
    }

    val screenTitle = if (transactionToEdit == null) "Nuovo Movimento" else "Modifica Movimento"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = ::validateAndSave,
                icon = { Icon(Icons.Default.Save, contentDescription = "Salva") },
                text = { Text("Salva") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        // COLUMN PRINCIPALE
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- SELETTORE TIPO (SPESA/ENTRATA) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf("expense" to "Spesa", "income" to "Entrata").forEach { (t, label) ->
                    ChoiceChip(
                        label = label,
                        isSelected = type == t,
                        onClick = { type = t },
                        color = if (t == "expense") Color(0xFFB71C1C) else Color(0xFF2E7D32)
                    )
                    Spacer(Modifier.width(8.dp))
                }
            }

            // --- CAMPI PRINCIPALI ---

            // 1. Importo
            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it.replace(',', '.') },
                label = { Text("Importo") },
                prefix = { Text(currencySymbol) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            // 2. Data Movimento
            DatePickerButton(
                label = "Data Movimento (Acquisto)",
                selectedDate = date,
                onDateChange = { date = it }
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 3. Data Addebito
            DatePickerButton(
                label = "Data Addebito (Effettiva)",
                selectedDate = effectiveDate,
                onDateChange = { effectiveDate = it }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 4. Descrizione con Autocomplete
            // Poiché AnimatedVisibility usa animazioni verticali (expand/shrink),
            // DEVE trovarsi all'interno di un ColumnScope, che è fornito da questo Column.
            // Usiamo il Column per raggruppare il campo di testo e la lista suggerimenti.
            Column(Modifier.fillMaxWidth().zIndex(1f)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrizione") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            // Mostra/Nascondi la lista dei suggerimenti in base al focus e alla presenza di testo
                            if (!focusState.isFocused) {
                                scope.launch {
                                    kotlinx.coroutines.delay(100) // Breve ritardo per permettere il click sul suggerimento
                                    showSuggestions = false
                                }
                            } else {
                                showSuggestions = suggestions.isNotEmpty() && description.length > 1
                            }
                        }
                )

                // Lista Suggerimenti (Autocomplete)
                // Questa è la chiamata che richiede il ColumnScope
                AnimatedVisibility( // <--- RIGA CHE RISOLVE L'ERRORE (La sua posizione in questo file è ~198)
                    visible = showSuggestions,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        shadowElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(suggestions) { suggestion ->
                                Column { // Necessario per usare HorizontalDivider
                                    Text(
                                        text = suggestion,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                description = suggestion
                                                showSuggestions = false
                                                focusManager.clearFocus()
                                            }
                                            .padding(12.dp)
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            } // Fine Column Autocomplete
            Spacer(modifier = Modifier.height(16.dp))

            // 5. SELEZIONE CATEGORIA (I CHIPS)
            val currentCategory = CATEGORIES.find { it.id == categoryId } ?: CATEGORIES.last { it.id == "altro" }
            CategoryChipSelector(
                categories = CATEGORIES,
                selectedCategory = currentCategory,
                onCategorySelected = { category -> categoryId = category.id }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 6. Carta di Credito (Checkbox)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isCreditCard = !isCreditCard }
                    .padding(vertical = 8.dp)
            ) {
                Checkbox(
                    checked = isCreditCard,
                    onCheckedChange = { isCreditCard = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pagato con Carta di Credito (Addebito differito)")
            }
        }
    }
}

// --- COMPONENTI RIUTILIZZABILI ---

@Composable
fun DatePickerButton(
    label: String,
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog.value = true }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = selectedDate.format(dateFormatter),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
    HorizontalDivider()

    if (showDialog.value) {
        SimpleDatePickerDialog(
            initialDate = selectedDate,
            onDismiss = { showDialog.value = false },
            onConfirm = { newDate ->
                onDateChange(newDate)
                showDialog.value = false
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    // Conversione di LocalDate in Millisecondi
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Conversione di Millisecondi in LocalDate
                        val selectedLocalDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        onConfirm(selectedLocalDate)
                    }
                }
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun ChoiceChip(label: String, isSelected: Boolean, onClick: () -> Unit, color: Color) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.Bold) },
        leadingIcon = {
            if (label == "Spesa") Icon(Icons.Default.ArrowUpward, null) else Icon(Icons.Default.ArrowDownward, null)
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isSelected) color else color.copy(alpha = 0.1f),
            labelColor = if (isSelected) Color.White else color,
            leadingIconContentColor = if (isSelected) Color.White else color
        ),
        border = if (!isSelected) BorderStroke(1.dp, color.copy(alpha = 0.5f)) else null
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChipSelector(
    categories: List<Category>,
    selectedCategory: Category,
    onCategorySelected: (Category) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Seleziona Categoria:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = category == selectedCategory,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category.name) },
                    leadingIcon = {
                        Text(
                            text = category.name,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                        containerColor = Color(0xFFF0F0F0),
                        labelColor = Color.Gray,
                    )
                )
            }
        }
    }
}