package com.alessandrogregorio.gestorespese.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentCurrency: String,
    ccDelay: Int,
    ccLimit: Float,
    onCurrencyChange: (String) -> Unit,
    onDelayChange: (Int) -> Unit,
    onLimitChange: (Float) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onExportCsv: () -> Unit // NUOVO PARAMETRO
) {
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var limitStr by remember { mutableStateOf(String.format(Locale.US, "%.0f", ccLimit)) }

    // Sincronizza lo stato locale con ccLimit esterno
    LaunchedEffect(ccLimit) {
        limitStr = String.format(Locale.US, "%.0f", ccLimit)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Impostazioni", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // SEZIONE GENERALI
        Text("Generali", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Selettore Valuta
        ListItem(
            headlineContent = { Text("Valuta") },
            supportingContent = { Text("Simbolo attuale: $currentCurrency") },
            leadingContent = { Icon(Icons.Default.ShoppingCart, null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
            modifier = Modifier.clickable { showCurrencyDialog = true }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SEZIONE CARTA DI CREDITO
        Text("Carta di Credito", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {

                // Plafond Mensile
                Text("Plafond Mensile (Limite Spesa)", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = limitStr,
                    onValueChange = {
                        limitStr = it.replace(",", ".") // Sostituisci la virgola con il punto
                        it.replace(",", ".").toFloatOrNull()?.let { num ->
                            if(num >= 0) onLimitChange(num)
                        }
                    },
                    prefix = { Text(currentCurrency) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Posticipo Addebito
                Text("Posticipo Addebito: $ccDelay mesi", fontWeight = FontWeight.Bold)
                Slider(
                    value = ccDelay.toFloat(),
                    onValueChange = { onDelayChange(it.toInt()) },
                    valueRange = 0f..6f, // AUMENTATO A 6 MESI
                    steps = 6
                )
                Text(
                    text = when(ccDelay) {
                        0 -> "Addebito nello stesso mese dell'acquisto"
                        1 -> "Addebito il mese successivo"
                        else -> "Addebito dopo $ccDelay mesi"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // SEZIONE DATI
        Text("Dati e Backup", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Nuovo Bottone Esporta CSV
        Button(
            onClick = onExportCsv, // COLLEGATO AL NUOVO HANDLER
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Default.Download, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Esporta Spese in CSV")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Vecchi bottoni Backup/Ripristino
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onBackup,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Backup JSON")
            }

            Button(
                onClick = onRestore,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ripristina JSON")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Dialog Selezione Valuta
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Scegli Valuta") },
            text = {
                Column {
                    listOf("€", "$", "£", "CHF", "¥", "zł").forEach { symbol ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCurrencyChange(symbol)
                                    showCurrencyDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (symbol == currentCurrency), onClick = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(symbol, fontSize = 20.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCurrencyDialog = false }) { Text("Annulla") } }
        )
    }
}