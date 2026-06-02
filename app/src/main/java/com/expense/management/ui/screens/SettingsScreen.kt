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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
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
import com.expense.management.data.PaymentMethodEntity
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
    enabledWidgets: Set<com.expense.management.domain.model.DashboardWidget>,
    onEnabledWidgetsChange: (Set<com.expense.management.domain.model.DashboardWidget>) -> Unit,
    allPaymentMethods: List<PaymentMethodEntity> = emptyList(),
    defaultPaymentMethodId: String = "__cash__",
    onDefaultPaymentMethodChange: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showCurrencyRatesInfoDialog by remember { mutableStateOf(false) }

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

        val updateText = if (lastRatesUpdate != null && lastRatesUpdate > 0) {
            val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastRatesUpdate), ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            stringResource(R.string.last_update, date.format(formatter))
        } else {
            stringResource(R.string.no_rates_downloaded)
        }

        val appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "N/A"

        AccordionSection(
            title = stringResource(R.string.general),
            initiallyExpanded = false,
            icon = Icons.Default.AttachMoney,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = stringResource(R.string.currency),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (hasTransactions) {
                    Text(
                        text = stringResource(R.string.currency_change_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 32.dp, top = 2.dp, bottom = 4.dp),
                    )
                }
                listOf("€", "\$", "£", "CHF", "¥", "zł").forEach { symbol ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !hasTransactions) {
                                onCurrencyChange(symbol)
                            }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = symbol == currentCurrency,
                            onClick = null,
                            enabled = !hasTransactions,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(symbol, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                )

                settingsListItem(
                    icon = Icons.Default.CurrencyExchange,
                    title = stringResource(R.string.currency_rates),
                    value = updateText,
                    onClick = { showCurrencyRatesInfoDialog = true },
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = stringResource(R.string.date_format),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.date_format),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                val dateFormats = listOf("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "dd-MM-yyyy")
                dateFormats.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDateFormatChange(format) }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = format == currentDateFormat, onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(format, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AccordionSection(
            title = stringResource(R.string.theme),
            initiallyExpanded = false,
            icon = Icons.Default.BrightnessMedium,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.BrightnessMedium,
                        contentDescription = stringResource(R.string.theme),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.theme),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                val themeModes = listOf("system", "light", "dark")
                val themeLabels = mapOf(
                    "system" to stringResource(R.string.theme_system),
                    "light" to stringResource(R.string.theme_light),
                    "dark" to stringResource(R.string.theme_dark),
                )
                themeModes.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeModeChange(mode) }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == currentThemeMode, onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(themeLabels[mode] ?: mode, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = stringResource(R.string.app_style),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.app_style),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                AppStyle.entries.forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAppStyleChange(style) }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = style == currentAppStyle, onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
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
        }

        Spacer(modifier = Modifier.height(12.dp))

        AccordionSection(
            title = stringResource(R.string.payment_methods),
            initiallyExpanded = false,
            icon = Icons.Default.CreditCard,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = stringResource(R.string.default_payment_method),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.default_payment_method),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                allPaymentMethods.forEach { method ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDefaultPaymentMethodChange(method.id)
                            }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = method.id == defaultPaymentMethodId,
                            onClick = {
                                onDefaultPaymentMethodChange(method.id)
                            },
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(method.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                )

                settingsListItem(
                    icon = Icons.Default.CreditCard,
                    title = stringResource(R.string.payment_methods),
                    value = stringResource(R.string.manage_payment_methods),
                    onClick = onNavigateToPaymentMethods,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AccordionSection(
            title = stringResource(R.string.management),
            initiallyExpanded = false,
            icon = Icons.Default.Backup,
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
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = stringResource(R.string.dashboard_widgets),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.dashboard_widgets),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                var selectedWidgets by remember(enabledWidgets) { mutableStateOf(enabledWidgets) }
                com.expense.management.domain.model.DashboardWidget.entries.forEach { widget ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedWidgets = if (selectedWidgets.contains(widget)) {
                                    selectedWidgets - widget
                                } else {
                                    selectedWidgets + widget
                                }
                            }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = selectedWidgets.contains(widget),
                            onCheckedChange = { isChecked ->
                                selectedWidgets = if (isChecked) {
                                    selectedWidgets + widget
                                } else {
                                    selectedWidgets - widget
                                }
                            },
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            when (widget) {
                                com.expense.management.domain.model.DashboardWidget.SUMMARY_CARDS -> "Summary Cards"
                                com.expense.management.domain.model.DashboardWidget.CREDIT_CARD_INFO -> "Credit Card Info"
                                com.expense.management.domain.model.DashboardWidget.BNPL_PROJECTIONS -> "BNPL Projections"
                                com.expense.management.domain.model.DashboardWidget.TRANSACTION_LIST -> "Transaction List"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                TextButton(
                    onClick = {
                        onEnabledWidgetsChange(selectedWidgets)
                    },
                    modifier = Modifier.padding(start = 28.dp, top = 4.dp),
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AccordionSection(
            title = stringResource(R.string.csv_export_settings),
            initiallyExpanded = false,
            icon = Icons.Default.Description,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = stringResource(R.string.customize_csv_export),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.customize_csv_export),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                var selectedColumns by remember(csvExportColumns) { mutableStateOf(csvExportColumns) }
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
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 32.dp),
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
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(displayName, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                TextButton(
                    onClick = {
                        onCsvExportColumnsChange(selectedColumns)
                    },
                    modifier = Modifier.padding(start = 28.dp, top = 4.dp),
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AccordionSection(
            title = stringResource(R.string.about),
            initiallyExpanded = false,
            icon = Icons.Default.Info,
        ) {
            Column {
                settingsListItem(
                    icon = Icons.Default.Description,
                    title = stringResource(R.string.privacy_policy),
                    value = stringResource(R.string.privacy_policy_desc),
                    onClick = {
                        val privacyPolicyUrl = "https://gist.github.com/sal3984/adc05b7037705f169aa6682b877ef581"
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
                    onClick = { },
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

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
}

@Preview(showBackground = true, name = "Settings Light")
@Composable
private fun SettingsPreview() {
    gestoreSpeseTheme(darkTheme = false, dynamicColor = false) {
        settingsScreen(
            currentCurrency = "€", currentDateFormat = "dd/MM/yyyy", currentThemeMode = "system",
            currentAppStyle = AppStyle.MATERIAL_YOU, csvExportColumns = emptySet(), hasTransactions = false,
            currencyRates = emptyList(), lastRatesUpdate = null, allCreditCards = emptyList(),
            onRefreshCurrencyRates = {}, onForceCurrencyRatesUpdate = {}, onAddCreditCard = {},
            onUpdateCreditCard = {}, onDeleteCreditCard = {}, onCurrencyChange = {},
            onDateFormatChange = {}, onCcPaymentModeChange = {}, onCsvExportColumnsChange = {},
            onThemeModeChange = {}, onAppStyleChange = {}, onNavigateToDataManagement = {},
            onNavigateToSecurity = {}, onNavigateToPaymentMethods = {},
            enabledWidgets = com.expense.management.domain.model.DashboardWidget.entries.toSet(),
            onEnabledWidgetsChange = {},
            allPaymentMethods = emptyList(),
            defaultPaymentMethodId = "__cash__",
            onDefaultPaymentMethodChange = {},
        )
    }
}

@Preview(showBackground = true, name = "Settings Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsPreviewDark() {
    gestoreSpeseTheme(darkTheme = true, dynamicColor = false) {
        settingsScreen(
            currentCurrency = "€", currentDateFormat = "dd/MM/yyyy", currentThemeMode = "system",
            currentAppStyle = AppStyle.MATERIAL_YOU, csvExportColumns = emptySet(), hasTransactions = false,
            currencyRates = emptyList(), lastRatesUpdate = null, allCreditCards = emptyList(),
            onRefreshCurrencyRates = {}, onForceCurrencyRatesUpdate = {}, onAddCreditCard = {},
            onUpdateCreditCard = {}, onDeleteCreditCard = {}, onCurrencyChange = {},
            onDateFormatChange = {}, onCcPaymentModeChange = {}, onCsvExportColumnsChange = {},
            onThemeModeChange = {}, onAppStyleChange = {}, onNavigateToDataManagement = {},
            onNavigateToSecurity = {}, onNavigateToPaymentMethods = {},
            enabledWidgets = com.expense.management.domain.model.DashboardWidget.entries.toSet(),
            onEnabledWidgetsChange = {},
            allPaymentMethods = emptyList(),
            defaultPaymentMethodId = "__cash__",
            onDefaultPaymentMethodChange = {},
        )
    }
}
