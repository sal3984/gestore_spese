package com.expense.management.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import com.expense.management.R
import com.expense.management.data.CreditCardEntity
import com.expense.management.data.CurrencyRate
import com.expense.management.ui.theme.AppStyle
import com.expense.management.ui.theme.gestoreSpeseTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    "DataAddebito" to "Data Addebito",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun settingsScreen(
    modifier: Modifier = Modifier,
    currentCurrency: String,
    currentDateFormat: String,
    currentThemeMode: String,
    currentAppStyle: AppStyle,
    csvExportColumns: Set<String>,
    hasTransactions: Boolean,
    currencyRates: List<CurrencyRate>,
    lastRatesUpdate: Long?,
    allCreditCards: List<CreditCardEntity>,
    onRefreshCurrencyRates: () -> Unit,
    onForceCurrencyRatesUpdate: suspend () -> Unit,
    onAddCreditCard: (CreditCardEntity) -> Unit,
    onUpdateCreditCard: (CreditCardEntity) -> Unit,
    onDeleteCreditCard: (CreditCardEntity) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onDateFormatChange: (String) -> Unit,
    onCcPaymentModeChange: (String) -> Unit,
    onCsvExportColumnsChange: (Set<String>) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onAppStyleChange: (AppStyle) -> Unit,
    onNavigateToDataManagement: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToPaymentMethods: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAppStyleDialog by remember { mutableStateOf(false) }
    var showExportColumnsDialog by remember { mutableStateOf(false) }
    var showCurrencyWarningDialog by remember { mutableStateOf(false) }
    var showCurrencyRatesInfoDialog by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var showEditCardDialog by remember { mutableStateOf<CreditCardEntity?>(null) }
    var showDeleteCardDialog by remember { mutableStateOf<CreditCardEntity?>(null) }

    LaunchedEffect(Unit) {
        onRefreshCurrencyRates()
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
                    onClick = {
                        if (hasTransactions) {
                            showCurrencyWarningDialog = true
                        } else {
                            showCurrencyDialog = true
                        }
                    },
                    // Valuta non cliccabile se ci sono transazioni
                    isClickable = !hasTransactions,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Voce Tassi di Cambio
                val updateText = if (lastRatesUpdate != null && lastRatesUpdate > 0) {
                    val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastRatesUpdate), ZoneId.systemDefault())
                    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    stringResource(R.string.last_update, date.format(formatter))
                } else {
                    stringResource(R.string.no_rates_downloaded)
                }

                settingsListItem(
                    icon = Icons.Default.CurrencyExchange,
                    title = stringResource(R.string.currency_rates),
                    value = updateText,
                    onClick = { showCurrencyRatesInfoDialog = true },
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

        // --- SEZIONE TEMA ---
        settingsSectionHeader(stringResource(R.string.theme))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            val themeLabel = when (currentThemeMode) {
                "light" -> stringResource(R.string.theme_light)
                "dark" -> stringResource(R.string.theme_dark)
                else -> stringResource(R.string.theme_system)
            }
            settingsListItem(
                icon = Icons.Default.BrightnessMedium,
                title = stringResource(R.string.theme),
                value = themeLabel,
                onClick = { showThemeDialog = true },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            val styleLabel = when (currentAppStyle) {
                AppStyle.MATERIAL_YOU -> stringResource(R.string.style_material_you)
                AppStyle.NORDIC -> stringResource(R.string.style_nordic)
                AppStyle.CYBERPUNK -> stringResource(R.string.style_cyberpunk)
                AppStyle.CORPORATE -> stringResource(R.string.style_corporate)
            }
            settingsListItem(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.app_style),
                value = styleLabel,
                onClick = { showAppStyleDialog = true },
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- SEZIONE GESTIONE ---
        settingsSectionHeader(stringResource(R.string.management))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column {
                settingsListItem(
                    icon = Icons.Default.Backup,
                    title = stringResource(R.string.data_management),
                    value = stringResource(R.string.data_management_subtitle),
                    onClick = onNavigateToDataManagement,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                settingsListItem(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.security_usability),
                    value = stringResource(R.string.app_lock_desc),
                    onClick = onNavigateToSecurity,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                settingsListItem(
                    icon = Icons.Default.CreditCard,
                    title = stringResource(R.string.payment_methods),
                    value = stringResource(R.string.manage_payment_methods),
                    onClick = onNavigateToPaymentMethods,
                )
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

        // --- ABOUT SECTION ---
        settingsSectionHeader(stringResource(R.string.about))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
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
                        val githubRepoUrl = "https://github.com/sal3984/gestore_spese"
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
                                .heightIn(min = 48.dp)
                                .clickable {
                                    onCurrencyChange(symbol)
                                    showCurrencyDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = (symbol == currentCurrency), onClick = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(symbol, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCurrencyDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    // Dialog Warning Valuta
    if (showCurrencyWarningDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyWarningDialog = false },
            title = { Text(stringResource(R.string.cannot_change_currency)) },
            text = { Text(stringResource(R.string.currency_change_warning)) },
            confirmButton = {
                TextButton(onClick = { showCurrencyWarningDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }

    // Dialog Info Tassi Cambio
    if (showCurrencyRatesInfoDialog) {
        Dialog(onDismissRequest = { showCurrencyRatesInfoDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .padding(16.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.currency_rates_info_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    var isRefreshing by remember { mutableStateOf(false) }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isRefreshing = true
                                onForceCurrencyRatesUpdate()
                                isRefreshing = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isRefreshing,
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.updating))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.force_update))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.available_rates_against_eur),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    HorizontalDivider()

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                    ) {
                        items(currencyRates) { rate ->
                            ListItem(
                                headlineContent = { Text(rate.currencyCode, fontWeight = FontWeight.Bold) },
                                trailingContent = { Text(String.format(Locale.US, "%.4f", rate.rateAgainstEuro)) },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { showCurrencyRatesInfoDialog = false },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    }

    // Dialog Selezione Tema
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.choose_theme)) },
            text = {
                Column {
                    val themeModes = listOf("system", "light", "dark")
                    val themeLabels = mapOf(
                        "system" to stringResource(R.string.theme_system),
                        "light" to stringResource(R.string.theme_light),
                        "dark" to stringResource(R.string.theme_dark),
                    )
                    themeModes.forEach { mode ->
                        Row(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable {
                                    onThemeModeChange(mode)
                                    showThemeDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = (mode == currentThemeMode), onClick = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(themeLabels[mode] ?: mode, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    // Dialog Selezione Stile
    if (showAppStyleDialog) {
        AlertDialog(
            onDismissRequest = { showAppStyleDialog = false },
            title = { Text(stringResource(R.string.choose_app_style)) },
            text = {
                Column {
                    AppStyle.entries.forEach { style ->
                        Row(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable {
                                    onAppStyleChange(style)
                                    showAppStyleDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = (style == currentAppStyle), onClick = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                when (style) {
                                    AppStyle.MATERIAL_YOU -> stringResource(R.string.style_material_you)
                                    AppStyle.NORDIC -> stringResource(R.string.style_nordic)
                                    AppStyle.CYBERPUNK -> stringResource(R.string.style_cyberpunk)
                                    AppStyle.CORPORATE -> stringResource(R.string.style_corporate)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAppStyleDialog = false }) { Text(stringResource(R.string.cancel)) } },
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
                                .heightIn(min = 48.dp)
                                .clickable {
                                    onDateFormatChange(format)
                                    showDateFormatDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = (format == currentDateFormat), onClick = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(format, style = MaterialTheme.typography.bodyLarge)
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
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
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
            dismissButton = { TextButton(onClick = { showExportColumnsDialog = false }) { Text(stringResource(R.string.cancel)) } },
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
    isClickable: Boolean = true,
) {
    val alpha = if (isClickable) 1f else 0.5f
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = alpha),
                modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha), CircleShape)
                    .padding(10.dp),
            )
        },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.more),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
        },
        modifier =
        Modifier
            .let { if (isClickable) it.clickable(onClick = onClick) else it }
            .padding(vertical = 4.dp),
    )
}

@Preview(showBackground = true, name = "Settings Light")
@Composable
private fun SettingsPreview() {
    gestoreSpeseTheme(darkTheme = false, dynamicColor = false) {
        settingsScreen(currentCurrency = "€", currentDateFormat = "dd/MM/yyyy", currentThemeMode = "system", currentAppStyle = AppStyle.MATERIAL_YOU, csvExportColumns = emptySet(), hasTransactions = false, currencyRates = emptyList(), lastRatesUpdate = null, allCreditCards = emptyList(), onRefreshCurrencyRates = {}, onForceCurrencyRatesUpdate = {}, onAddCreditCard = {}, onUpdateCreditCard = {}, onDeleteCreditCard = {}, onCurrencyChange = {}, onDateFormatChange = {}, onCcPaymentModeChange = {}, onCsvExportColumnsChange = {}, onThemeModeChange = {}, onAppStyleChange = {}, onNavigateToDataManagement = {}, onNavigateToSecurity = {}, onNavigateToPaymentMethods = {})
    }
}

@Preview(showBackground = true, name = "Settings Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsPreviewDark() {
    gestoreSpeseTheme(darkTheme = true, dynamicColor = false) {
        settingsScreen(currentCurrency = "€", currentDateFormat = "dd/MM/yyyy", currentThemeMode = "system", currentAppStyle = AppStyle.MATERIAL_YOU, csvExportColumns = emptySet(), hasTransactions = false, currencyRates = emptyList(), lastRatesUpdate = null, allCreditCards = emptyList(), onRefreshCurrencyRates = {}, onForceCurrencyRatesUpdate = {}, onAddCreditCard = {}, onUpdateCreditCard = {}, onDeleteCreditCard = {}, onCurrencyChange = {}, onDateFormatChange = {}, onCcPaymentModeChange = {}, onCsvExportColumnsChange = {}, onThemeModeChange = {}, onAppStyleChange = {}, onNavigateToDataManagement = {}, onNavigateToSecurity = {}, onNavigateToPaymentMethods = {})
    }
}
