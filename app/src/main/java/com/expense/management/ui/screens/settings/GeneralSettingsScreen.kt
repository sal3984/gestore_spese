package com.expense.management.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.expense.management.R
import com.expense.management.data.CurrencyRate
import com.expense.management.ui.screens.settingsSectionHeader
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GeneralSettingsScreen(
    currentCurrency: String,
    currentDateFormat: String,
    hasTransactions: Boolean,
    currencyRates: List<CurrencyRate>,
    lastRatesUpdate: Long?,
    onCurrencyChange: (String) -> Unit,
    onDateFormatChange: (String) -> Unit,
    onRefreshCurrencyRates: () -> Unit,
    onForceCurrencyRatesUpdate: suspend () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var showCurrencyRatesInfoDialog by remember { mutableStateOf(false) }

    val updateText = if (lastRatesUpdate != null && lastRatesUpdate > 0) {
        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastRatesUpdate), ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        stringResource(R.string.last_update, date.format(formatter))
    } else {
        stringResource(R.string.no_rates_downloaded)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        settingsSectionHeader(stringResource(R.string.currency))

        if (hasTransactions) {
            Text(
                text = stringResource(R.string.currency_change_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        val currencies = listOf("€", "\$", "£", "CHF", "¥", "zł")
        currencies.forEach { symbol ->
            ListItem(
                headlineContent = { Text(symbol, style = MaterialTheme.typography.bodyLarge) },
                leadingContent = {
                    Icon(
                        Icons.Default.AttachMoney,
                        contentDescription = symbol,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                },
                trailingContent = {
                    RadioButton(
                        selected = symbol == currentCurrency,
                        onClick = null,
                        enabled = !hasTransactions,
                    )
                },
                modifier = Modifier
                    .clickable(enabled = !hasTransactions) { onCurrencyChange(symbol) }
                    .heightIn(min = 48.dp),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        ListItem(
            headlineContent = {
                Text(
                    stringResource(R.string.currency_rates),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            supportingContent = { Text(updateText, style = MaterialTheme.typography.bodySmall) },
            leadingContent = {
                Icon(
                    Icons.Default.CurrencyExchange,
                    contentDescription = stringResource(R.string.currency_rates),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            },
            modifier = Modifier.clickable { showCurrencyRatesInfoDialog = true },
        )

        Spacer(modifier = Modifier.height(24.dp))

        settingsSectionHeader(stringResource(R.string.date_format))

        val dateFormats = listOf("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "dd-MM-yyyy")
        dateFormats.forEach { format ->
            ListItem(
                headlineContent = { Text(format, style = MaterialTheme.typography.bodyLarge) },
                leadingContent = {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = format,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                },
                trailingContent = {
                    RadioButton(
                        selected = format == currentDateFormat,
                        onClick = null,
                    )
                },
                modifier = Modifier
                    .clickable { onDateFormatChange(format) }
                    .heightIn(min = 48.dp),
            )
        }
    }

    if (showCurrencyRatesInfoDialog) {
        CurrencyRatesInfoDialog(
            currencyRates = currencyRates,
            onForceUpdate = {
                coroutineScope.launch {
                    onForceCurrencyRatesUpdate()
                }
            },
            onDismiss = { showCurrencyRatesInfoDialog = false },
        )
    }
}

@Composable
private fun CurrencyRatesInfoDialog(
    currencyRates: List<CurrencyRate>,
    onForceUpdate: suspend () -> Unit,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
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

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isRefreshing = true
                            onForceUpdate()
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

                LazyColumn(modifier = Modifier.weight(1f)) {
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
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}
