package com.alessandrogregorio.gestorespese.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentCurrency: String,
    currentDateFormat: String,
    ccDelay: Int,
    ccLimit: Float,
    isAmountHidden: Boolean, // 👈 NUOVO PARAMETRO
    onCurrencyChange: (String) -> Unit,
    onDateFormatChange: (String) -> Unit,
    onDelayChange: (Int) -> Unit,
    onLimitChange: (Float) -> Unit,
    onAmountHiddenChange: (Boolean) -> Unit, // 👈 NUOVO CALLBACK
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onExportCsv: () -> Unit
) {
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    var limitStr by remember { mutableStateOf(String.format(Locale.US, "%.0f", ccLimit)) }

    // Sincronizza lo stato locale di limitStr con il valore ccLimit
    LaunchedEffect(ccLimit) {
        limitStr = String.format(Locale.US, "%.0f", ccLimit)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "Impostazioni",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        // --- SEZIONE GENERALI ---
        Text("Generali", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                // Valuta
                ListItem(
                    headlineContent = { Text("Valuta") },
                    supportingContent = { Text("Simbolo visualizzato: $currentCurrency") },
                    leadingContent = {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showCurrencyDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Formato Data
                ListItem(
                    headlineContent = { Text("Formato Data") },
                    supportingContent = { Text(currentDateFormat) },
                    leadingContent = {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showDateFormatDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SEZIONE CARTA DI CREDITO ---
        Text("Carta di Credito", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Plafond
                OutlinedTextField(
                    value = limitStr,
                    onValueChange = {
                        limitStr = it.replace(',', '.')
                        val newLimit = limitStr.toFloatOrNull()
                        if (newLimit != null) onLimitChange(newLimit)
                    },
                    label = { Text("Plafond Max ($currentCurrency)") },
                    leadingIcon = { Icon(Icons.Default.CreditCard, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Ritardo Addebito
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                     Text("Ritardo Addebito", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                     Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                         Text(
                             if (ccDelay == 0) "Immediato" else "$ccDelay Mesi",
                             modifier = Modifier.padding(4.dp),
                             color = MaterialTheme.colorScheme.onSecondaryContainer
                         )
                     }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = ccDelay.toFloat(),
                    onValueChange = { onDelayChange(it.toInt()) },
                    valueRange = 0f..3f, // Ridotto range per realismo
                    steps = 2
                )
                Text(
                    "Sposta l'addebito delle spese CC al mese successivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // NUOVA SEZIONE: Sicurezza e Usabilità
        Text(
            "SICUREZZA E USABILITÀ",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
        )

        // Opzione 1: Oscurare i numeri
        SettingItem(
            icon = Icons.Default.VisibilityOff,
            title = "Oscura Importi per Privacy",
            subtitle = "Sostituisce gli importi con asterischi nella Dashboard.",
            onClick = { onAmountHiddenChange(!isAmountHidden) }
        ) {
            Switch(checked = isAmountHidden, onCheckedChange = onAmountHiddenChange)
        }

        // Opzione 2: Placeholder per Sicurezza (PIN/Biometrica)
        SettingItem(
            icon = Icons.Default.Security,
            title = "Blocco App (PIN/Biometrica)",
            subtitle = "Abilita il blocco schermo all'avvio dell'app (DA IMPLEMENTARE).",
            onClick = { /* TODO: Implementare Logica Blocco */ }
        ) {
            // Placeholder per Switch Sicurezza
            Switch(checked = false, onCheckedChange = { /* TODO: Implementare */ }, enabled = false)
        }


        // --- SEZIONE DATI ---
        Text("Gestione Dati", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        // Esporta CSV
        Button(
            onClick = onExportCsv,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.Download, null)
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text("Esporta CSV", style = MaterialTheme.typography.titleSmall)
                Text("Salva un report delle spese", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Backup & Ripristino (JSON)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onBackup,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), // Green
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Icon(Icons.Default.CloudUpload, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Backup")
            }

            Button(
                onClick = onRestore,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)), // Orange
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Icon(Icons.Default.CloudDownload, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ripristina")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Dialog Selezione Valuta
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Scegli Simbolo Valuta") },
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

    // Dialog Selezione Formato Data
    if (showDateFormatDialog) {
        AlertDialog(
            onDismissRequest = { showDateFormatDialog = false },
            title = { Text("Scegli Formato Data") },
            text = {
                Column {
                    val dateFormats = listOf("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "dd-MM-yyyy")
                    dateFormats.forEach { format ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDateFormatChange(format)
                                    showDateFormatDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (format == currentDateFormat), onClick = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(format, fontSize = 18.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDateFormatDialog = false }) { Text("Annulla") } }
        )
    }
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    content: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icona
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Titolo e Sottotitolo
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Contenuto a destra (es. Switch, Arrow, o nulla)
        content?.invoke() ?: Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    // Aggiungi un Divider per separare le voci
    Divider(modifier = Modifier.padding(horizontal = 16.dp))
}
