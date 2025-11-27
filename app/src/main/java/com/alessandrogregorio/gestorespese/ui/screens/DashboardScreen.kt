package com.alessandrogregorio.gestorespese.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alessandrogregorio.gestorespese.R
import com.alessandrogregorio.gestorespese.data.TransactionEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    ccLimit: Float,
    dateFormat: String,
    earliestMonth: YearMonth,
    currentDashboardMonth: YearMonth, // Parametro aggiunto
    onMonthChange: (YearMonth) -> Unit, // Callback aggiunta
    onDelete: (String) -> Unit,
    onEdit: (String) -> Unit,
    isAmountHidden: Boolean, // NUOVO PARAMETRO
) {
    val today = YearMonth.now()
    // var currentDisplayedMonth by remember { mutableStateOf(today) } // RIMOSSO STATO LOCALE
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    val currentTrans = transactions
        .filter {
            try {
                YearMonth.from(LocalDate.parse(it.effectiveDate)) == currentDashboardMonth // Uso parametro
            } catch (e: Exception) {
                false
            }
        }
        .sortedByDescending { it.effectiveDate }

    val groupedTransactions = remember(currentTrans) {
        currentTrans.groupBy { it.effectiveDate }
    }

    val totalIncome = currentTrans.filter { it.type == "income" }.sumOf { it.amount }
    val totalExpense = currentTrans.filter { it.type == "expense" }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    val isViewingCurrentMonth = currentDashboardMonth == today
    val creditCardUsed = transactions
        .filter { it.isCreditCard && it.type == "expense" }
        .filter {
            try {
                YearMonth.from(LocalDate.parse(it.effectiveDate)) > today
            } catch (e: Exception) {
                false
            }
        }
        .sumOf { it.amount }

    val ccProgress = if (ccLimit > 0) (creditCardUsed / ccLimit).toFloat() else 0f

    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {

        // --- HEADER: Mese e Saldo Totale ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
                .padding(bottom = 32.dp) // Spazio extra per l'overlap
        ) {
             // Navigazione Mese
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onMonthChange(currentDashboardMonth.minusMonths(1)) }, // Uso callback
                    enabled = currentDashboardMonth > earliestMonth
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_month), tint = Color.White)
                }

                Text(
                    currentDashboardMonth.format(monthFormatter).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(
                    onClick = { onMonthChange(currentDashboardMonth.plusMonths(1)) }, // Uso callback
                    enabled = currentDashboardMonth < today
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_month), tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Saldo Centrale
             Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.monthly_balance),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(Locale.getDefault(), "%.2f", netBalance)}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- CONTENUTO (Overlap sul Header) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (-30).dp) // Overlap effect
                .padding(horizontal = 16.dp)
        ) {

            // Card Entrate/Uscite
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Entrate
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowUpward, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.income), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(Locale.getDefault(), "%.2f", totalIncome)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Uscite
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowDownward, null, tint = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.expenses), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(Locale.getDefault(), "%.2f", totalExpense)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card Carta di Credito (Se attiva)
            if (ccLimit > 0 && isViewingCurrentMonth) {
                 Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                             Icon(Icons.Default.CreditCard, null, tint = MaterialTheme.colorScheme.primary)
                             Spacer(modifier = Modifier.width(8.dp))
                             Text(stringResource(R.string.credit_card_limit_label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { ccProgress.coerceAtMost(1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (ccProgress > 0.8f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                             Text(
                                text = if (isAmountHidden) "${stringResource(R.string.spent_label)} $currencySymbol *****" else "${stringResource(R.string.spent_label)} $currencySymbol ${String.format(Locale.getDefault(), "%.0f", creditCardUsed)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                             Text(
                                "${stringResource(R.string.limit_label)} $currencySymbol ${String.format(Locale.getDefault(), "%.0f", ccLimit)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                 }
                 Spacer(modifier = Modifier.height(16.dp))
            }

            // Lista Transazioni
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                groupedTransactions.forEach { (dateString, transactions) ->
                    stickyHeader {
                         DateHeader(dateString)
                    }
                    items(transactions, key = { it.id }) { t ->
                        TransactionItem(
                            t,
                            currencySymbol,
                            dateFormat,
                            isAmountHidden, // NUOVO ARGOMENTO QUI
                            onDelete,
                            onEdit
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeader(dateString: String) {
    val date = try { LocalDate.parse(dateString) } catch(e: Exception) { LocalDate.now() }
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    val label = when(date) {
        today -> stringResource(R.string.today)
        yesterday -> stringResource(R.string.yesterday)
        else -> date.format(DateTimeFormatter.ofPattern("dd MMMM", Locale.getDefault()))
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}
