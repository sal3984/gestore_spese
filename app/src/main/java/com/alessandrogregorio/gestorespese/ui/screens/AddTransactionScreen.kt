package com.alessandrogregorio.gestorespese.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alessandrogregorio.gestorespese.data.TransactionEntity
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Le categorie restano qui per la selezione
data class Category(val id: String, val label: String, val icon: String)
val CATEGORIES = listOf(
    Category("food", "Cibo \uD83C\uDF7D️", "Cibo"),
    Category("transport", "Trasporti \uD83D\uDE97", "Auto"),
    Category("housing", "Casa \uD83C\uDFE0", "Casa"),
    Category("entertainment", "Svago \uD83C\uDFC1", "Svago"),
    Category("salary", "Stipendio \uD83D\uDCB0", "Stipendio"),
    Category("income", "Altro Entrata \uD83D\uDCB8", "Entrata"),
    Category("other", "Altro \uD83C\uDFC6", "Altro")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    ccDelay: Int,
    currencySymbol: String,
    onGetSuggestions: suspend (String) -> List<String>,
    onSave: (TransactionEntity) -> Unit,
    transactionToEdit: TransactionEntity? = null // NUOVO: Transazione da modificare
) {
    val isEditing = transactionToEdit != null

    // Inizializza gli stati con i dati della transazione se in modalità modifica
    var type by remember { mutableStateOf(transactionToEdit?.type ?: "expense") }
    var description by remember { mutableStateOf(transactionToEdit?.description ?: "") }
    var amountStr by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var categoryId by remember { mutableStateOf(transactionToEdit?.categoryId ?: "food") }
    var isCreditCard by remember { mutableStateOf(transactionToEdit?.isCreditCard ?: false) }
    val dateStr = transactionToEdit?.date ?: LocalDate.now().toString() // Mantiene la data originale se in modifica

    // Gestione Autocomplete
    var suggestions by remember { mutableStateOf(emptyList<String>()) }
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(if(isEditing) "Modifica Movimento" else "Nuovo Movimento", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Tasto Toggle Spesa/Entrata
        Row(modifier = Modifier.fillMaxWidth().background(Color.LightGray.copy(alpha=0.3f), RoundedCornerShape(8.dp))) {
            Button(
                onClick = { type = "expense" },
                modifier = Modifier.weight(1f),
                enabled = !isEditing, // Impedisce il cambio di tipo in modifica
                colors = ButtonDefaults.buttonColors(containerColor = if(type == "expense") MaterialTheme.colorScheme.primary else Color.Transparent)
            ) { Text("Spesa", color = if(type=="expense") Color.White else Color.Black) }

            Button(
                onClick = { type = "income"; isCreditCard = false },
                modifier = Modifier.weight(1f),
                enabled = !isEditing, // Impedisce il cambio di tipo in modifica
                colors = ButtonDefaults.buttonColors(containerColor = if(type == "income") Color(0xFF2E7D32) else Color.Transparent)
            ) { Text("Entrata", color = if(type=="income") Color.White else Color.Black) }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // CAMPO DESCRIZIONE CON AUTOCOMPLETE
        ExposedDropdownMenuBox(
            expanded = expanded && suggestions.isNotEmpty(),
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    scope.launch {
                        suggestions = onGetSuggestions(it)
                        expanded = suggestions.isNotEmpty() && it.isNotEmpty()
                    }
                },
                label = { Text("Descrizione") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                singleLine = true
            )

            ExposedDropdownMenu(
                expanded = expanded && suggestions.isNotEmpty(),
                onDismissRequest = { expanded = false }
            ) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            description = suggestion
                            expanded = false
                            suggestions = emptyList()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = amountStr,
            onValueChange = { amountStr = it.replace(",",".") },
            label = { Text("Importo ($currencySymbol)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selezione Categoria
        val cats = if(type=="income") CATEGORIES.filter{it.id=="income"} else CATEGORIES.filter{it.id!="income"}
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            cats.forEach { cat ->
                FilterChip(selected = categoryId == cat.id, onClick = { categoryId = cat.id }, label = { Text(
                    cat.label
                ) }, modifier = Modifier.padding(end=8.dp))
            }
        }

        // Opzione Carta di Credito
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
                        LocalDate.parse(dateStr).plusMonths(ccDelay.toLong()).toString()
                    else dateStr

                    // Se siamo in modalità modifica, usiamo l'ID esistente
                    val idToSave = transactionToEdit?.id ?: 0L

                    onSave(TransactionEntity(id = idToSave, date = dateStr, description = description, amount = amount, categoryId = categoryId, type = type, isCreditCard = isCreditCard, effectiveDate = effectiveDate))
                }
            },
            enabled = amountStr.toDoubleOrNull() != null && description.isNotBlank()
        ) { Text(if(isEditing) "AGGIORNA MOVIMENTO" else "SALVA MOVIMENTO") }
    }
}