package com.expense.management.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expense.management.R
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentCurrency: String,
    currentDateFormat: String,
    ccDelay: Int,
    ccLimit: Float,
    isAmountHidden: Boolean,
    isBiometricEnabled: Boolean,
    onCurrencyChange: (String) -> Unit,
    onDateFormatChange: (String) -> Unit,
    onDelayChange: (Int) -> Unit,
    onLimitChange: (Float) -> Unit,
    onAmountHiddenChange: (Boolean) -> Unit,
    onBiometricEnabledChange: (Boolean) -> Unit,
) {
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    var limitStr by remember { mutableStateOf(String.format(Locale.US, "%.0f", ccLimit)) }

    LaunchedEffect(ccLimit) {
        limitStr = String.format(Locale.US, "%.0f", ccLimit)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {

        // --- SEZIONE GENERALI ---
        SettingsSectionHeader(stringResource(R.string.general))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                SettingsListItem(
                    icon = Icons.Default.AttachMoney,
                    title = stringResource(R.string.currency),
                    value = stringResource(R.string.displayed_symbol, currentCurrency),
                    onClick = { showCurrencyDialog = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                SettingsListItem(
                    icon = Icons.Default.CalendarToday,
                    title = stringResource(R.string.date_format),
                    value = currentDateFormat,
                    onClick = { showDateFormatDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SEZIONE PAGAMENTI (Carta di Credito) ---
        SettingsSectionHeader(stringResource(R.string.credit_card_settings))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    label = { Text(stringResource(R.string.max_limit, currentCurrency)) },
                    leadingIcon = { Icon(Icons.Default.CreditCard, null, tint = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Ritardo Addebito
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                     Text(stringResource(R.string.debit_delay), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                     Surface(
                         color = MaterialTheme.colorScheme.primaryContainer,
                         shape = RoundedCornerShape(8.dp)
                     ) {
                         Text(
                             if (ccDelay == 0) stringResource(R.string.immediate) else stringResource(R.string.months_delay, ccDelay),
                             modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                             style = MaterialTheme.typography.labelMedium,
                             color = MaterialTheme.colorScheme.onPrimaryContainer,
                             fontWeight = FontWeight.Bold
                         )
                     }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = ccDelay.toFloat(),
                    onValueChange = { onDelayChange(it.toInt()) },
                    valueRange = 0f..3f,
                    steps = 2,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    stringResource(R.string.cc_delay_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SEZIONE SICUREZZA & PRIVACY ---
        SettingsSectionHeader(stringResource(R.string.security_usability))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                SettingsSwitchItem(
                    icon = Icons.Default.VisibilityOff,
                    title = stringResource(R.string.hide_amounts),
                    subtitle = stringResource(R.string.hide_amounts_desc),
                    checked = isAmountHidden,
                    onCheckedChange = onAmountHiddenChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                SettingsSwitchItem(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.app_lock),
                    subtitle = stringResource(R.string.app_lock_desc),
                    checked = isBiometricEnabled,
                    onCheckedChange = onBiometricEnabledChange
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Dialog Selezione Valuta
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text(stringResource(R.string.choose_currency_symbol)) },
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
            confirmButton = { TextButton(onClick = { showCurrencyDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    // Dialog Selezione Formato Data
    if (showDateFormatDialog) {
        AlertDialog(
            onDismissRequest = { showDateFormatDialog = false },
            title = { Text(stringResource(R.string.choose_date_format)) },
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
            confirmButton = { TextButton(onClick = { showDateFormatDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

// --- Componenti Helper per Settings ---

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsListItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape).padding(8.dp)
            )
        },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape).padding(8.dp)
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}
