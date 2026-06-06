package com.expense.management.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.data.CurrencyRate
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.domain.model.DashboardWidget
import com.expense.management.ui.screens.settings.AboutSettingsScreen
import com.expense.management.ui.screens.settings.DisplaySettingsScreen
import com.expense.management.ui.screens.settings.GeneralSettingsScreen
import com.expense.management.ui.screens.settings.PaymentSettingsScreen
import com.expense.management.ui.screens.settings.ThemeSettingsScreen
import com.expense.management.ui.theme.AppStyle
import com.expense.management.ui.theme.AppTheme

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

private enum class SettingsSection {
    GENERAL,
    THEME,
    PAYMENT,
    DISPLAY,
    ABOUT,
}

@Composable
fun settingsScreen(
    currentCurrency: String,
    currentDateFormat: String,
    currentThemeMode: String,
    currentAppStyle: AppStyle,
    csvExportColumns: Set<String>,
    hasTransactions: Boolean,
    currencyRates: List<CurrencyRate>,
    lastRatesUpdate: Long?,
    onRefreshCurrencyRates: () -> Unit,
    onForceCurrencyRatesUpdate: suspend () -> Unit,
    onCurrencyChange: (String) -> Unit,
    onDateFormatChange: (String) -> Unit,
    onCsvExportColumnsChange: (Set<String>) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onAppStyleChange: (AppStyle) -> Unit,
    onNavigateToDataManagement: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToPaymentMethods: () -> Unit,
    enabledWidgets: Set<DashboardWidget>,
    onEnabledWidgetsChange: (Set<DashboardWidget>) -> Unit,
    allPaymentMethods: List<PaymentMethodEntity>,
    defaultPaymentMethodId: String,
    onDefaultPaymentMethodChange: (String) -> Unit,
) {
    var selectedSection by rememberSaveable { mutableStateOf<SettingsSection?>(null) }

    if (selectedSection == null) {
        SettingsList(
            onSectionSelected = { selectedSection = it },
            onNavigateToDataManagement = onNavigateToDataManagement,
            onNavigateToSecurity = onNavigateToSecurity,
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                settingsSectionHeader(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    IconButton(onClick = { selectedSection = null }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                    Text(
                        text = sectionTitle(selectedSection!!),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                HorizontalDivider()

                when (selectedSection!!) {
                    SettingsSection.GENERAL -> GeneralSettingsScreen(
                        currentCurrency = currentCurrency,
                        currentDateFormat = currentDateFormat,
                        hasTransactions = hasTransactions,
                        currencyRates = currencyRates,
                        lastRatesUpdate = lastRatesUpdate,
                        onCurrencyChange = onCurrencyChange,
                        onDateFormatChange = onDateFormatChange,
                        onRefreshCurrencyRates = onRefreshCurrencyRates,
                        onForceCurrencyRatesUpdate = onForceCurrencyRatesUpdate,
                    )
                    SettingsSection.THEME -> ThemeSettingsScreen(
                        currentThemeMode = currentThemeMode,
                        currentAppStyle = currentAppStyle,
                        onThemeModeChange = onThemeModeChange,
                        onAppStyleChange = onAppStyleChange,
                    )
                    SettingsSection.PAYMENT -> PaymentSettingsScreen(
                        allPaymentMethods = allPaymentMethods,
                        defaultPaymentMethodId = defaultPaymentMethodId,
                        onDefaultPaymentMethodChange = onDefaultPaymentMethodChange,
                        onNavigateToPaymentMethods = onNavigateToPaymentMethods,
                    )
                    SettingsSection.DISPLAY -> DisplaySettingsScreen(
                        enabledWidgets = enabledWidgets,
                        csvExportColumns = csvExportColumns,
                        onEnabledWidgetsChange = onEnabledWidgetsChange,
                        onCsvExportColumnsChange = onCsvExportColumnsChange,
                        hasAmex = allPaymentMethods.any { it.provider == com.expense.management.domain.model.PaymentProvider.CREDIT_CARD_AMEX },
                    )
                    SettingsSection.ABOUT -> AboutSettingsScreen()
                }
            }
        }
    }
}

@Composable
private fun settingsSectionHeader(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun SettingsList(
    onSectionSelected: (SettingsSection) -> Unit,
    onNavigateToDataManagement: () -> Unit,
    onNavigateToSecurity: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                settingsSectionHeader(
                    title = stringResource(R.string.general),
                    icon = Icons.Default.AttachMoney,
                    onClick = { onSectionSelected(SettingsSection.GENERAL) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                settingsSectionHeader(
                    title = stringResource(R.string.theme),
                    icon = Icons.Default.BrightnessMedium,
                    onClick = { onSectionSelected(SettingsSection.THEME) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                settingsSectionHeader(
                    title = stringResource(R.string.payment_methods),
                    icon = Icons.Default.CreditCard,
                    onClick = { onSectionSelected(SettingsSection.PAYMENT) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                settingsSectionHeader(
                    title = stringResource(R.string.display_widgets_csv),
                    icon = Icons.Default.Description,
                    onClick = { onSectionSelected(SettingsSection.DISPLAY) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(R.string.data_management),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    supportingContent = {
                        Text(
                            stringResource(R.string.data_management_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Backup,
                            contentDescription = stringResource(R.string.data_management),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.more),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    modifier = Modifier.clickable { onNavigateToDataManagement() },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(R.string.security_usability),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    supportingContent = {
                        Text(
                            stringResource(R.string.app_lock_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = stringResource(R.string.security_usability),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.more),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    modifier = Modifier.clickable { onNavigateToSecurity() },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        stringResource(R.string.about),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = stringResource(R.string.about),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.more),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.clickable { onSectionSelected(SettingsSection.ABOUT) },
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun settingsSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        leadingContent = {
            Icon(
                icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.more),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier.clickable { onClick() },
    )
}

@Composable
private fun sectionTitle(section: SettingsSection): String = when (section) {
    SettingsSection.GENERAL -> stringResource(R.string.general)
    SettingsSection.THEME -> stringResource(R.string.theme)
    SettingsSection.PAYMENT -> stringResource(R.string.payment_methods)
    SettingsSection.DISPLAY -> stringResource(R.string.display)
    SettingsSection.ABOUT -> stringResource(R.string.about)
}

@Preview(showBackground = true, name = "Settings List Light")
@Composable
private fun SettingsListPreview() {
    AppTheme(appStyle = AppStyle.MATERIAL_YOU, darkTheme = false) {
        SettingsList(
            onSectionSelected = {},
            onNavigateToDataManagement = {},
            onNavigateToSecurity = {},
        )
    }
}
