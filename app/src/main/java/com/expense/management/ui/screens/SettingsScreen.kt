package com.expense.management.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    ccPaymentMode: String,
    onCurrencyChange: (String) -> Unit,
    onDateFormatChange: (String) -> Unit,
    onDelayChange: (Int) -> Unit,
    onLimitChange: (Float) -> Unit,
    onCcPaymentModeChange: (String) -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToDataManagement: () -> Unit
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
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                // NUOVI LINK A SECURITY E DATA MANAGEMENT
                SettingsListItem(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.security_usability),
                    value = stringResource(R.string.app_lock) + ", " + stringResource(R.string.hide_amounts),
                    onClick = onNavigateToSecurity
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                SettingsListItem(
                    icon = Icons.Default.Backup,
                    title = stringResource(R.string.data_management),
                    value = stringResource(R.string.backup) + ", " + stringResource(R.string.restore) + ", CSV",
                    onClick = onNavigateToDataManagement
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

                // Modalità Pagamento (Saldo vs Rateale)
                Text(
                    stringResource(R.string.credit_card_mode),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = ccPaymentMode == "single",
                        onClick = { onCcPaymentModeChange("single") },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text(stringResource(R.string.single_balance))
                    }
                    SegmentedButton(
                        selected = ccPaymentMode == "installment",
                        onClick = { onCcPaymentModeChange("installment") },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text(stringResource(R.string.installment_plan))
                    }
                    SegmentedButton(
                        selected = ccPaymentMode == "manual",
                        onClick = { onCcPaymentModeChange("manual") },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text(stringResource(R.string.manual))
                    }
                }
                Text(
                    when(ccPaymentMode) {
                        "single" -> stringResource(R.string.setting_credit_card_message_1)
                        "installment" -> stringResource(R.string.setting_credit_card_message_2)
                        else -> stringResource(R.string.setting_credit_card_message_3)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Ritardo Addebito (Solo per Saldo Unico)
                AnimatedVisibility(visible = ccPaymentMode == "single") {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(R.string.debit_delay),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    if (ccDelay == 0) stringResource(R.string.immediate) else stringResource(
                                        R.string.months_delay,
                                        ccDelay
                                    ),
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
            }
        }
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
