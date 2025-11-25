package com.alessandrogregorio.gestorespese.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alessandrogregorio.gestorespese.data.TransactionEntity
import com.alessandrogregorio.gestorespese.ui.screens.category.CATEGORIES
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Definiamo un formato data standard per l'app
private val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    ccDelay: Int,
    currencySymbol: String,
    onGetSuggestions: suspend (String) -> List<String>,
    onSave: (TransactionEntity) -> Unit,
    transactionToEdit: TransactionEntity? = null
) {
    val isEditing = transactionToEdit != null

    // Stato per gli input
    var type by remember { mutableStateOf(transactionToEdit?.type ?: "expense") }
    var description by remember { mutableStateOf(transactionToEdit?.description ?: "") }
    var amountStr by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var dateStr by remember { mutableStateOf(transactionToEdit?.date ?: LocalDate.now().toString()) }
    var categoryId by remember { mutableStateOf(transactionToEdit?.categoryId ?: CATEGORIES.first { it.label != "Stipendio 💰" }.id) }
    var isCreditCard by remember { mutableStateOf(transactionToEdit?.isCreditCard ?: false) }

    // Stato per il Date Picker
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            LocalDate.parse(dateStr).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            Instant.now().toEpochMilli()
        }
    )

    val coroutineScope = rememberCoroutineScope()
    var suggestions by remember { mutableStateOf(emptyList<String>()) }
    var showSuggestions by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            if(isEditing) "Modifica Movimento" else "Nuovo Movimento",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // TIPO: Spesa/Entrata (omitted for brevity, assume the code block below is here)
        // ... TIPO: Spesa/Entrata ...
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            FilterChip(
                selected = type == "expense",
                onClick = { type = "expense" },
                label = { Text("Spesa") }
            )
            FilterChip(
                selected = type == "income",
                onClick = {
                    type = "income"
                    isCreditCard = false // Entrate non sono mai con Carta di Credito
                    if (categoryId == CATEGORIES.first { it.label != "Stipendio 💰" }.id) { // Se è la categoria di default
                        categoryId = CATEGORIES.first { it.label == "Stipendio 💰" }.id
                    }
                },
                label = { Text("Entrata") }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // DESCRIZIONE (omitted for brevity, assume the code block below is here)
        // ... DESCRIZIONE ...
        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
                coroutineScope.launch {
                    if (it.length >= 2) {
                        suggestions = onGetSuggestions(it)
                        showSuggestions = suggestions.isNotEmpty()
                    } else {
                        showSuggestions = false
                    }
                }
            },
            label = { Text("Descrizione") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        // Autocomplete
        if (showSuggestions && suggestions.isNotEmpty()) {
            suggestions.forEach { suggestion ->
                Text(
                    text = suggestion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            description = suggestion
                            showSuggestions = false
                        }
                        .padding(8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // IMPORTO (omitted for brevity, assume the code block below is here)
        // ... IMPORTO ...
        OutlinedTextField(
            value = amountStr,
            onValueChange = { amountStr = it.replace(',', '.').filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Importo ($currencySymbol)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))


        // ** CAMPO DATA AGGIORNATO CON DATE PICKER **
        OutlinedTextField(
            value = dateStr,
            onValueChange = { dateStr = it }, // Permette ancora la digitazione manuale
            label = { Text("Data (AAAA-MM-GG)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            readOnly = true, // Opzionale: rende il campo data solo cliccabile, non digitabile
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Seleziona Data")
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        // ** FINE CAMPO DATA AGGIORNATO **

        // CATEGORIA (omitted for brevity, assume the code block below is here)
        // ... CATEGORIA ...
        Text("Categoria:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filteredCategories = CATEGORIES.filter { cat ->
                if (type == "expense") cat.label != "Stipendio 💰"
                else cat.label == "Stipendio 💰" || cat.label == "Altro (Entrata) ➕"
            }

            filteredCategories.forEach { cat ->
                AssistChip(
                    onClick = { categoryId = cat.id },
                    label = { Text(cat.label) },
                    leadingIcon = { Text(cat.icon) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (categoryId == cat.id) MaterialTheme.colorScheme.primary else Color.LightGray,
                        labelColor = if (categoryId == cat.id) Color.White else Color.Black
                    )
                )
            }
        }


        // Opzione Carta di Credito (omitted for brevity, assume the code block below is here)
        // ... Opzione Carta di Credito ...
        if(type == "expense") {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isCreditCard, onCheckedChange = { isCreditCard = it })
                Text("Carta di Credito (Posticipo $ccDelay mesi)")
            }
        }


        Spacer(modifier = Modifier.height(32.dp))
        Button(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            onClick = {
                amountStr.toDoubleOrNull()?.let { amount ->
                    if(description.isBlank()) return@let

                    val effectiveDate = if (isCreditCard && type == "expense")
                        LocalDate.parse(dateStr, dateFormat).plusMonths(ccDelay.toLong()).toString()
                    else dateStr

                    // Se siamo in modalità modifica, usiamo l'ID esistente (Long).
                    // Altrimenti, 0L con autoGenerate = true in TransactionEntity
                    val idToSave = transactionToEdit?.id ?: 0L

                    onSave(TransactionEntity(id = idToSave, date = dateStr, description = description, amount = amount, categoryId = categoryId, type = type, isCreditCard = isCreditCard, effectiveDate = effectiveDate))
                }
            },
            enabled = amountStr.toDoubleOrNull() != null && description.isNotBlank() && dateStr.isNotBlank() // Aggiunta validazione data
        ) { Text(if(isEditing) "AGGIORNA MOVIMENTO" else "SALVA MOVIMENTO") }

        // DatePicker Dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            // Converte il timestamp in LocalDate e poi in stringa AAAA-MM-GG
                            dateStr = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormat)
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
