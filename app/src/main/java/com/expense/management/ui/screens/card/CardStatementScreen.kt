package com.expense.management.ui.screens.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.domain.model.CardStatement
import com.expense.management.domain.model.StatementCycle
import com.expense.management.domain.model.StatementEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Estratto di una carta a saldo: plafond, prossimo addebito, ciclo in corso e storico. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardStatementScreen(
    statement: CardStatement?,
    currencySymbol: String,
    locale: Locale,
    isAmountHidden: Boolean,
    onAlignSettlement: (cycleId: String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val format = remember(currencySymbol, locale, isAmountHidden) { AmountFormat(currencySymbol, locale, isAmountHidden) }
    // La Scaffold principale applica gia' i margini di sistema: qui non vanno ripetuti
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Column {
                        Text(statement?.cardName ?: stringResource(R.string.card_statement_title))
                        if (statement != null) {
                            Text(
                                stringResource(R.string.card_statement_cycle_days, statement.closingDay, statement.paymentDay),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { innerPadding ->
        if (statement == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { LimitHeader(statement, format) }
            val noCycles = statement.upcomingCharges.isEmpty() && statement.openCycles.isEmpty() && statement.history.isEmpty()
            if (noCycles) {
                item {
                    Text(
                        stringResource(R.string.card_statement_no_cycles),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (statement.upcomingCharges.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.card_statement_upcoming)) }
                items(statement.upcomingCharges, key = { it.cycleId }) { cycle ->
                    val subtitle = statement.linkedAccountName?.let {
                        stringResource(R.string.card_statement_closed_charge_account, cycle.closingDate.short(), cycle.dueDate.short(), it)
                    } ?: stringResource(R.string.card_statement_closed_charge, cycle.closingDate.short(), cycle.dueDate.short())
                    CycleCard(cycle, subtitle, format, locale, highlighted = true)
                }
            }
            if (statement.openCycles.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.card_statement_open)) }
                items(statement.openCycles, key = { it.cycleId }) { cycle ->
                    val subtitle = stringResource(R.string.card_statement_open_charge, cycle.closingDate.short(), cycle.dueDate.short())
                    CycleCard(cycle, subtitle, format, locale, highlighted = false)
                }
            }
            if (statement.history.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.card_statement_history)) }
                items(statement.history, key = { it.cycleId }) { cycle ->
                    HistoryRow(cycle, format, locale, onAlignSettlement)
                }
            }
        }
    }
}

@Composable
private fun LimitHeader(statement: CardStatement, format: AmountFormat) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.card_statement_limit_used), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.card_statement_limit, format(statement.limit)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { statement.exposure.utilization.coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (statement.exposure.utilization > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric(stringResource(R.string.card_statement_to_charge), format(statement.exposure.exposure), Modifier.weight(1f))
                Metric(stringResource(R.string.card_statement_available), format(statement.exposure.available), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun CycleCard(cycle: StatementCycle, subtitle: String, format: AmountFormat, locale: Locale, highlighted: Boolean) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            CycleTitle(cycle, format, locale)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            if (cycle.entries.isEmpty() && cycle.scheduledEntries.isEmpty()) {
                Text(stringResource(R.string.card_statement_no_purchases), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            cycle.entries.forEach { EntryRow(it, format, scheduled = false) }
            cycle.scheduledEntries.forEach { EntryRow(it, format, scheduled = true) }
        }
    }
}

@Composable
private fun CycleTitle(cycle: StatementCycle, format: AmountFormat, locale: Locale) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.card_statement_cycle_of, cycle.cycleMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(format(cycle.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EntryRow(entry: StatementEntry, format: AmountFormat, scheduled: Boolean) {
    val color = if (scheduled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${entry.description} · ${entry.date.short()}",
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (scheduled) {
            Text(
                stringResource(R.string.card_statement_scheduled),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        Text(format(if (entry.isCredit) -entry.amount else entry.amount), style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun HistoryRow(cycle: StatementCycle, format: AmountFormat, locale: Locale, onAlignSettlement: (String) -> Unit) {
    var expanded by remember(cycle.cycleId) { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (cycle.settlementMismatch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.clickable { expanded = !expanded }.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        cycle.cycleMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) },
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        stringResource(R.string.card_statement_charged_on, cycle.dueDate.short()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (cycle.settledBeforeUpdate) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Text(
                            stringResource(R.string.card_statement_before_update),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(format(cycle.settledAmount ?: cycle.total), modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleSmall)
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (cycle.settlementMismatch && cycle.settledAmount != null) {
                Text(
                    stringResource(R.string.card_statement_mismatch, format(cycle.total), format(cycle.settledAmount)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
                TextButton(onClick = { onAlignSettlement(cycle.cycleId) }, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.card_statement_align))
                }
            }
            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                cycle.entries.forEach { EntryRow(it, format, scheduled = false) }
            }
        }
    }
}

private val shortDate = DateTimeFormatter.ofPattern("dd/MM")

private fun LocalDate.short(): String = format(shortDate)

private class AmountFormat(private val currencySymbol: String, private val locale: Locale, private val hidden: Boolean) {
    operator fun invoke(amount: Double): String =
        if (hidden) "$currencySymbol *****" else "$currencySymbol ${String.format(locale, "%.2f", amount)}"
}
