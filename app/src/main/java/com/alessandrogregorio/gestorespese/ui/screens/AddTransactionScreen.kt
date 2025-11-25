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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alessandrogregorio.gestorespese.data.TransactionEntity
import com.alessandrogregorio.gestorespese.ui.screens.category.CATEGORIES
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    ccDelay: Int,
    currencySymbol: String,
    dateFormatString: String,
    onGetSuggestions: suspend (String) -> List<String>,
    onSave: (TransactionEntity) -> Unit,
    transactionToEdit: TransactionEntity? = null
) {
    val isEditing = transactionToEdit != null
    val displayFormatter = remember(dateFormatString) { DateTimeFormatter.ofPattern(dateFormatString, Locale.ITALIAN) }
    val dbFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun parseDate(dateStr: String, formatter: DateTimeFormatter): LocalDate? {
        return try {
            LocalDate.parse(dateStr, formatter)
        } catch (e: DateTimeParseException) {
            try {
                LocalDate.parse(dateStr, dbFormatter)
            } catch (e: DateTimeParseException) {
                null
            }
        }
    }

    val initialDateDisplay = transactionToEdit?.date?.let { dbDate ->
        parseDate(dbDate, dbFormatter)?.format(displayFormatter)
    } ?: LocalDate.now().format(displayFormatter)

    var type by remember { mutableStateOf(transactionToEdit?.type ?: "expense") }
    var description by remember { mutableStateOf(transactionToEdit?.description ?: "") }
    var amountStr by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var dateStr by remember { mutableStateOf(initialDateDisplay) }
    var categoryId by remember { mutableStateOf(transactionToEdit?.categoryId ?: CATEGORIES.first { it.label != "Stipendio 💰" }.id) }
    var isCreditCard by remember { mutableStateOf(transactionToEdit?.isCreditCard ?: false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val initialDateMillis = remember(dateStr) {
        parseDate(dateStr, displayFormatter)?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            ?: Instant.now().toEpochMilli()
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
    val coroutineScope = rememberCoroutineScope()
    var suggestions by remember { mutableStateOf(emptyList<String>()) }
    var showSuggestions by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            if(isEditing) "Modifica Movimento" else "Nuovo Movimento",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Toggle Tipo Transazione (Segmented Control Custom)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val expenseSelected = type == "expense"

            // Spesa
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (expenseSelected) MaterialTheme.colorScheme.error else Color.Transparent)
                    .clickable { type = "expense" },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Uscita",
                    color = if (expenseSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            // Entrata
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (!expenseSelected) Color(0xFF43A047) else Color.Transparent)
                    .clickable {
                        type = "income"
                        isCreditCard = false
                        if (categoryId == CATEGORIES.first { it.label != "Stipendio 💰" }.id) {
                            categoryId = CATEGORIES.first { it.label == "Stipendio 💰" }.id
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Entrata",
                    color = if (!expenseSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Card Input
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Importo
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.replace(',', '.').filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Importo") },
                    suffix = { Text(currencySymbol) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Descrizione
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
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (showSuggestions && suggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        suggestions.take(3).forEach { suggestion ->
                            Text(
                                text = suggestion,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        description = suggestion
                                        showSuggestions = false
                                    }
                                    .padding(12.dp)
                            )
                            if(suggestion != suggestions.last()) HorizontalDivider()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Data (con Box wrapper per fix matchParentSize)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { },
                        label = { Text("Data") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Seleziona Data")
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Categorie
        Text("Categoria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val filteredCategories = CATEGORIES.filter { cat ->
                if (type == "expense") cat.label != "Stipendio 💰"
                else cat.label == "Stipendio 💰" || cat.label == "Altro (Entrata) ➕"
            }

            filteredCategories.forEach { cat ->
                val isSelected = categoryId == cat.id
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { categoryId = cat.id }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .border(
                                width = if(isSelected) 0.dp else 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(cat.icon, fontSize = 28.sp)
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Black.copy(alpha = 0.1f))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        cat.label.split(" ").first(), // Mostra solo la prima parola per compattezza
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Carta di Credito
        if(type == "expense") {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp).clickable { isCreditCard = !isCreditCard },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isCreditCard, onCheckedChange = { isCreditCard = it })
                    Column {
                        Text("Paga con Carta di Credito", fontWeight = FontWeight.Medium)
                        if(ccDelay > 0) {
                            Text("Addebito posticipato di $ccDelay mesi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp))

        // Bottone Salva
        Button(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            onClick = {
                amountStr.toDoubleOrNull()?.let { amount ->
                    if(description.isBlank() || dateStr.isBlank()) return@let

                    val parsedDate = parseDate(dateStr, displayFormatter) ?: return@let
                    val dbDate = parsedDate.format(dbFormatter)
                    val effectiveDate = if (isCreditCard && type == "expense")
                        parsedDate.plusMonths(ccDelay.toLong()).format(dbFormatter)
                    else dbDate
                    val idToSave = transactionToEdit?.id ?: 0L

                    onSave(TransactionEntity(
                        id = idToSave,
                        date = dbDate,
                        description = description,
                        amount = amount,
                        categoryId = categoryId,
                        type = type,
                        isCreditCard = isCreditCard,
                        effectiveDate = effectiveDate
                    ))
                }
            },
            enabled = amountStr.toDoubleOrNull() != null && description.isNotBlank() && dateStr.isNotBlank(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if(isEditing) "AGGIORNA" else "SALVA", fontSize = 18.sp)
        }

        if (showDatePicker) {
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
            ) { DatePicker(state = datePickerState) }
        }
    }
}
