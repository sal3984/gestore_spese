package com.expense.management.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.expense.management.R
import java.util.Locale

val EXPORT_COLUMN_MAP = mapOf(
    "ID" to "ID",
    "Data" to "Data",
    "Descrizione" to "Descrizione",
    "ImportoConvertito" to "Importo (Convertito)",
    "ImportoOriginale" to "Importo Originale",
    "ValutaOriginale" to "Valuta Originale",
    "Categoria" to "Categoria",
    "Tipo" to "Tipo",
    "CartaDiCredito" to "Carta di Credito",
    "DataAddebito" to "Data Addebito"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun settingsScreen(
    currentCurrency: String,
    currentDateFormat: String,
    ccDelay: Int,
    ccLimit: Float,
    ccPaymentMode: String,
    csvExportColumns: Set<String>,
    onCurrencyChange: (String) -> Unit,
    onDateFormatChange: (String) -> Unit,
    onDelayChange: (Int) -> Unit,
    onLimitChange: (Float) -> Unit,
    onCcPaymentModeChange: (String) -> Unit,
    onCsvExportColumnsChange: (Set<String>) -> Unit
) {
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    var showExportColumnsDialog by remember { mutableStateOf(false) }
    var limitStr by remember { mutableStateOf(String.format(Locale.US, "%.0f", ccLimit)) }

    LaunchedEffect(ccLimit) {
        limitStr = String.format(Locale.US, "%.0f", ccLimit)
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // --- SEZIONE GENERALI ---
        settingsSectionHeader(stringResource(R.string.general))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column {
                settingsListItem(
                    icon = Icons.Default.AttachMoney,
                    title = stringResource(R.string.currency),
                    value = stringResource(R.string.displayed_symbol, currentCurrency),
                    onClick = { showCurrencyDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                settingsListItem(
                    icon = Icons.Default.CalendarToday,
                    title = stringResource(R.string.date_format),
                    value = currentDateFormat,
                    onClick = { showDateFormatDialog = true },
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- SEZIONE PAGAMENTI (Carta di Credito) ---
        settingsSectionHeader(stringResource(R.string.credit_card_settings))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
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
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Modalità Pagamento (Saldo vs Rateale)
                Text(
                    stringResource(R.string.credit_card_mode),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = ccPaymentMode == "single",
                        onClick = { onCcPaymentModeChange("single") },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    ) {
                        Text(stringResource(R.string.single_balance), maxLines = 1)
                    }
                    SegmentedButton(
                        selected = ccPaymentMode == "installment",
                        onClick = { onCcPaymentModeChange("installment") },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    ) {
                        Text(stringResource(R.string.installment_plan), maxLines = 1)
                    }
                    SegmentedButton(
                        selected = ccPaymentMode == "manual",
                        onClick = { onCcPaymentModeChange("manual") },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    ) {
                        Text(stringResource(R.string.manual), maxLines = 1)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    when (ccPaymentMode) {
                        "single" -> stringResource(R.string.setting_credit_card_message_1)
                        "installment" -> stringResource(R.string.setting_credit_card_message_2)
                        else -> stringResource(R.string.setting_credit_card_message_3)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Ritardo Addebito (Solo per Saldo Unico)
                AnimatedVisibility(visible = ccPaymentMode == "single") {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(R.string.debit_delay),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    if (ccDelay == 0) {
                                        stringResource(R.string.immediate)
                                    } else {
                                        stringResource(
                                            R.string.months_delay,
                                            ccDelay,
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Slider(
                            value = ccDelay.toFloat(),
                            onValueChange = { onDelayChange(it.toInt()) },
                            valueRange = 0f..3f,
                            steps = 2,
                            colors =
                                SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                ),
                        )
                        Text(
                            stringResource(R.string.cc_delay_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- SEZIONE IMPOSTAZIONI ESPORTAZIONE CSV ---
        settingsSectionHeader(stringResource(R.string.csv_export_settings))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column {
                settingsListItem(
                    icon = Icons.Default.Description,
                    title = stringResource(R.string.customize_csv_export),
                    value = stringResource(R.string.selected_columns_count, csvExportColumns.size, EXPORT_COLUMN_MAP.size),
                    onClick = { showExportColumnsDialog = true },
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- ABOUT SECTION ---
        settingsSectionHeader(stringResource(R.string.about))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            val context = LocalContext.current
            val appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "N/A"

            Column {
                settingsListItem(
                    icon = Icons.Default.Description,
                    title = stringResource(R.string.privacy_policy),
                    value = stringResource(R.string.privacy_policy_desc),
                    onClick = {
                        val privacyPolicyUrl = "https://gist.github.com/sal3984/adc05b7037705f169aa6682b877ef581" // !!! REPLACE THIS WITH YOUR GIST URL !!!
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
                        context.startActivity(intent)
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                settingsListItem(
                    icon = Icons.Default.Code,
                    title = stringResource(R.string.github_repo),
                    value = stringResource(R.string.github_repo_desc),
                    onClick = {
                        val githubRepoUrl = "https://github.com/sal3984/gestore_spese" // !!! REPLACE THIS WITH YOUR GITHUB REPO URL !!!
                        val intent = Intent(Intent.ACTION_VIEW, githubRepoUrl.toUri())
                        context.startActivity(intent)
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                settingsListItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.app_name),
                    value = stringResource(R.string.app_version, appVersion),
                    onClick = { /* No action for clicking version */ },
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Dialog Selezione Valuta
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text(stringResource(R.string.choose_currency_symbol)) },
            text = {
                Column {
                    listOf("€", "\$", "£", "CHF", "¥", "zł").forEach { symbol ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCurrencyChange(symbol)
                                        showCurrencyDialog = false
                                    }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = (symbol == currentCurrency), onClick = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(symbol, fontSize = 20.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCurrencyDialog = false }) { Text(stringResource(R.string.cancel)) } },
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
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDateFormatChange(format)
                                        showDateFormatDialog = false
                                    }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = (format == currentDateFormat), onClick = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(format, fontSize = 18.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDateFormatDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    // Dialog Selezione Colonne Esportazione CSV
    if (showExportColumnsDialog) {
        var selectedColumns by remember { mutableStateOf(csvExportColumns) }
        AlertDialog(
            onDismissRequest = { showExportColumnsDialog = false },
            title = { Text(stringResource(R.string.select_columns_to_export)) },
            text = {
                Column {
                    EXPORT_COLUMN_MAP.entries.forEach { (key, displayName) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedColumns = if (selectedColumns.contains(key)) {
                                        selectedColumns - key
                                    } else {
                                        selectedColumns + key
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = selectedColumns.contains(key),
                                onCheckedChange = { isChecked ->
                                    selectedColumns = if (isChecked) {
                                        selectedColumns + key
                                    } else {
                                        selectedColumns - key
                                    }
                                },
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onCsvExportColumnsChange(selectedColumns)
                    showExportColumnsDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { showExportColumnsDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

// --- Componenti Helper per Settings ---

@Composable
fun settingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
    )
}

@Composable
fun settingsListItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .padding(10.dp),
            )
        },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier =
            Modifier
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
    )
}
