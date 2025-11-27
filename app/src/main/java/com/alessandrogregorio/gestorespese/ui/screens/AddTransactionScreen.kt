package com.alessandrogregorio.gestorespese.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alessandrogregorio.gestorespese.data.TransactionEntity
import com.alessandrogregorio.gestorespese.ui.screens.category.CATEGORIES
import com.alessandrogregorio.gestorespese.ui.screens.category.Category
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
    onSave: (TransactionEntity) -> Unit,
    onDelete: (String) -> Unit, // ID String (UUID)
    onBack: () -> Unit
) {
    // Stati Transazione
    var type by remember { mutableStateOf(transactionToEdit?.type ?: "expense") }
    var amountText by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(transactionToEdit?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(transactionToEdit?.categoryId ?: if (type == "expense") "food" else "salary") }
    var isCreditCard by remember { mutableStateOf(transactionToEdit?.isCreditCard ?: false) }

    // Stati per Valuta
    var originalAmountText by remember { mutableStateOf(transactionToEdit?.originalAmount?.toString() ?: "") }
    var originalCurrency by remember { mutableStateOf(transactionToEdit?.originalCurrency ?: currencySymbol) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

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

    val currentCategory: Category? = CATEGORIES.firstOrNull { it.id == selectedCategory }

    // Funzione di Salvataggio
    fun trySave() {
        val amount = try { amountText.replace(',', '.').toDouble() } catch (e: Exception) { 0.0 }
        val originalAmount = try { originalAmountText.replace(',', '.').toDouble() } catch (e: Exception) { amount }

        if (amount <= 0 || description.isBlank()) {
            scope.launch {
                snackbarHostState.showSnackbar("Inserisci un importo e una descrizione validi.", "OK")
            }
            return
        }

        val transactionDate: LocalDate = try {
            LocalDate.parse(dateStr, displayFormatter)
        } catch (e: DateTimeParseException) {
            scope.launch { snackbarHostState.showSnackbar("Formato data non valido.", "OK") }
            return
        }

        // GESTIONE ID ROBUSTA: Se è una modifica, usa l'ID esistente. Se è nuova, genera un nuovo UUID.
        val transactionId = transactionToEdit?.id ?: UUID.randomUUID().toString()


        // LOGICA AVVISO MESE PRECEDENTE
        val limitMonth = YearMonth.now().minusMonths(1)
        val transactionMonth = YearMonth.from(transactionDate)

        if (transactionMonth.isBefore(limitMonth)) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Impossibile operare su transazioni prima di ${limitMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN))}.",
                    "OK"
                )
            }
            return
        }

        if (transactionMonth.isBefore(YearMonth.now()) && transactionToEdit == null) {
            showPreviousMonthAlert = true
            return
        }

        // Se è un'operazione che non richiede l'avviso o l'utente ha confermato
        onSave(
            TransactionEntity(
                id = transactionId, // UTILIZZA L'ID CORRETTO GENERATO SOPRA
                date = transactionDate.format(displayFormatter),
                description = description.trim(),
                amount = amount,
                categoryId = selectedCategory,
                type = type,
                isCreditCard = isCreditCard,
                originalAmount = originalAmount,
                originalCurrency = originalCurrency,
                effectiveDate = calculateEffectiveDate(transactionDate, isCreditCard, ccDelay)
            )
        )
        onBack()
    }

    // UI Principale
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (transactionToEdit == null) "Aggiungi Movimento" else "Modifica Movimento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    if (transactionToEdit != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.height(72.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Button(
                    onClick = { trySave() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Salva", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (transactionToEdit == null) "SALVA MOVIMENTO" else "AGGIORNA MOVIMENTO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                        text = if (itemType == "expense") "SPESA" else "ENTRATA",
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                type = itemType
                                // Cambia categoria di default se cambia il tipo
                                if (currentCategory?.type != itemType) {
                                    selectedCategory = CATEGORIES.first { it.type == itemType }.id
                                }
                            }
                            .background(if (isSelected) color else Color.Transparent)
                            .padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Importo nella Valuta Principale
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.replace(',', '.') },
                label = { Text("Importo (${currencySymbol} - Converso)") },
                placeholder = { Text("100.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // NUOVO: Opzione Valuta Originale
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
                    label = { Text("Importo Orig.") },
                    placeholder = { Text("50.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Pulsante Valuta Originale
                OutlinedTextField(
                    value = originalCurrency,
                    onValueChange = { originalCurrency = it.uppercase(Locale.ROOT) },
                    label = { Text("Valuta Orig.") },
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier
                        .weight(0.7f)
                        .clickable { showCurrencyDialog = true }
                )
            }

            Text(
                text = "Inserisci l'importo nella valuta principale ($currencySymbol) nel campo superiore.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Campo Descrizione con Autocomplete
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    scope.launch {
                        // Per semplicità, non ho incluso la logica di ricerca suggerimenti qui,
                        // ma l'hai nel ViewModel e puoi implementarla con un DropdownMenu.
                    }
                },
                label = { Text("Descrizione") },
                placeholder = { Text("Cena fuori, Stipendio Gennaio...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selezione Categoria
            Text(
                "Categoria:",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                CATEGORIES.filter { it.type == type }.forEach { category ->
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
                Text("Pagamento con Carta di Credito", style = MaterialTheme.typography.bodyLarge)
            }

            // Data Transazione
            OutlinedTextField(
                value = dateStr,
                onValueChange = { dateStr = it },
                label = { Text("Data Transazione") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Seleziona Data")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Dialog Selezione Valuta
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Valuta Transazione Originale") },
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
            confirmButton = { TextButton(onClick = { showCurrencyDialog = false }) { Text("Annulla") } }
        )
    }

    // Dialog Avviso Mese Precedente
    if (showPreviousMonthAlert) {
        AlertDialog(
            onDismissRequest = { showPreviousMonthAlert = false },
            title = { Text("Attenzione: Data Passata") },
            text = { Text("Stai inserendo una nuova transazione nel mese precedente. Questa operazione modificherà i saldi storici. Sei sicuro di voler procedere?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPreviousMonthAlert = false
                        // Salva la transazione dopo la conferma
                        val amount = try { amountText.replace(',', '.').toDouble() } catch (e: Exception) { 0.0 }
                        val originalAmount = try { originalAmountText.replace(',', '.').toDouble() } catch (e: Exception) { amount }
                        val transactionDate = LocalDate.parse(dateStr, displayFormatter)
                        val transactionId = transactionToEdit?.id ?: UUID.randomUUID().toString()

                        onSave(
                            TransactionEntity(
                                id = transactionId,
                                date = dateStr,
                                description = description.trim(),
                                amount = amount,
                                categoryId = selectedCategory,
                                type = type,
                                isCreditCard = isCreditCard,
                                originalAmount = originalAmount,
                                originalCurrency = originalCurrency,
                                effectiveDate = calculateEffectiveDate(transactionDate, isCreditCard, ccDelay)
                            )
                        )
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("PROCEDI E SALVA")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreviousMonthAlert = false }) {
                    Text("ANNULLA")
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
            title = { Text("Elimina Movimento") },
            text = { Text("Sei sicuro di voler eliminare questa transazione? L'azione non può essere annullata.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionToEdit?.let { onDelete(it.id) }
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ELIMINA")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("ANNULLA")
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
