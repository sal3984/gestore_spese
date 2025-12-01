package com.expense.management.ui.screens

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
import androidx.compose.material.icons.filled.Warning
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
import com.expense.management.R
import com.expense.management.data.CategoryEntity
import com.expense.management.data.TransactionEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    ccLimit: Float,
    dateFormat: String,
    earliestMonth: YearMonth,
    currentDashboardMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (String) -> Unit,
    isAmountHidden: Boolean,
) {
    val today = YearMonth.now()
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    val currentTrans = transactions
        .filter {
            try {
                YearMonth.from(LocalDate.parse(it.effectiveDate, DateTimeFormatter.ISO_LOCAL_DATE)) == currentDashboardMonth
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
                YearMonth.from(LocalDate.parse(it.effectiveDate, DateTimeFormatter.ISO_LOCAL_DATE)) > today
            } catch (e: Exception) {
                false
            }
        }
        .sumOf { it.amount }

    val ccProgress = if (ccLimit > 0) (creditCardUsed / ccLimit).toFloat() else 0f

    // LIMITI NAVIGAZIONE:
    val minMonth = if (earliestMonth.isBefore(today.minusMonths(3))) earliestMonth else today.minusMonths(3)
    val maxMonth = today.plusMonths(12)

    // Unica LazyColumn per permettere lo scroll di tutta la pagina (anche header)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ITEM 1: Header + Cards
        item {
            Column {
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
                        .padding(bottom = 48.dp) // Increased space for overlap
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
                            onClick = { onMonthChange(currentDashboardMonth.minusMonths(1)) },
                            enabled = currentDashboardMonth > minMonth
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.previous_month),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        Text(
                            currentDashboardMonth.format(monthFormatter).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        IconButton(
                            onClick = { onMonthChange(currentDashboardMonth.plusMonths(1)) },
                            enabled = currentDashboardMonth < maxMonth
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.next_month),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Saldo Centrale
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.monthly_balance),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(Locale.getDefault(), "%.2f", netBalance)}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- CARDS ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-40).dp) // Adjusted overlap
                        .padding(horizontal = 16.dp)
                ) {
                    // Card Entrate/Uscite
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(24.dp), // More rounded
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Entrate
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        stringResource(R.string.income),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(Locale.getDefault(), "%.2f", totalIncome)}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            // Vertical Divider logic could go here if needed, but space is cleaner

                            // Uscite
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Align amounts to the end
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        stringResource(R.string.expenses),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(Locale.getDefault(), "%.2f", totalExpense)}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Box(modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Card Carta di Credito (Se attiva)
                    if (ccLimit > 0 && isViewingCurrentMonth) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CreditCard, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        stringResource(R.string.credit_card_limit_label),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (ccProgress > 0.9f) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    progress = { ccProgress.coerceAtMost(1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp)),
                                    color = if (ccProgress > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = if (isAmountHidden) "${stringResource(R.string.spent_label)} $currencySymbol *****" else "${stringResource(R.string.spent_label)} $currencySymbol ${String.format(Locale.getDefault(), "%.0f", creditCardUsed)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "${stringResource(R.string.limit_label)} $currencySymbol ${String.format(Locale.getDefault(), "%.0f", ccLimit)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }

        // Lista Transazioni
        if (groupedTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessuna transazione in questo mese.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            groupedTransactions.forEach { (dateString, transactions) ->
                stickyHeader {
                    DateHeader(dateString)
                }
                items(transactions, key = { it.id }) { t ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        TransactionItem(
                            transaction = t,
                            categories = categories,
                            currencySymbol = currencySymbol,
                            dateFormat = dateFormat,
                            isAmountHidden = isAmountHidden,
                            onDelete = onDelete,
                            onEdit = onEdit
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeader(dateString: String) {
    val date = try {
        LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch(e: Exception) {
        try {
            LocalDate.now()
        } catch(e2: Exception) {
            LocalDate.now()
        }
    }

    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    val label = when(date) {
        today -> stringResource(R.string.today)
        yesterday -> stringResource(R.string.yesterday)
        else -> date.format(DateTimeFormatter.ofPattern("dd MMMM", Locale.getDefault()))
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp) // Added horizontal padding
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}
