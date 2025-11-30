package com.expense.management.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expense.management.R
import com.expense.management.data.CategoryEntity
import com.expense.management.data.TransactionEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ReportScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    dateFormat: String,
    isAmountHidden: Boolean,
) {
    // --- 1. STATO DEL MESE SELEZIONATO ---
    var selectedReportMonth by remember { mutableStateOf<YearMonth?>(YearMonth.now()) }

    // Calcolo Risparmio Anno Corrente (Invariato)
    val currentYear = LocalDate.now().year
    val savings = transactions
        .filter {
            try {
                LocalDate.parse(it.effectiveDate).year == currentYear
            } catch (e: Exception) {
                false
            }
        }
        .sumOf { if(it.type == "income") it.amount else -it.amount }

    // --- 2. CALCOLO SPESE PER CATEGORIA (DINAMICO) ---
    val monthToShow = selectedReportMonth ?: YearMonth.now()

    val expenseByCategory = remember(transactions, monthToShow) {
        transactions
            .filter {
                it.type == "expense" && try {
                    YearMonth.from(LocalDate.parse(it.effectiveDate)) == monthToShow
                } catch (e: Exception) {
                    false
                }
            }
            .groupBy { it.categoryId }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val totalMonthlyExpense = expenseByCategory.sumOf { it.second }

    // Calcolo Bilancio Mensile (Anno Corrente)
    val monthlyBalances = remember(transactions, currentYear) {
        (1..12).map { month ->
            val targetMonth = YearMonth.of(currentYear, month)
            val monthlyTransactions = transactions.filter {
                try {
                    YearMonth.from(LocalDate.parse(it.effectiveDate)) == targetMonth
                } catch (e: Exception) {
                    false
                }
            }
            val income = monthlyTransactions.filter { it.type == "income" }.sumOf { it.amount }
            val expense = monthlyTransactions.filter { it.type == "expense" }.sumOf { it.amount }
            targetMonth to (income - expense)
        }
    }

    val scrollState = rememberScrollState()

    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(scrollState)
    ) {
        // --- HEADER ---
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
                .padding(24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                stringResource(R.string.report_year, currentYear),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        stringResource(R.string.total_savings),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(Locale.getDefault(), "%.2f", savings)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (savings >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    tint = Color.White
                )
            }
        }

        // --- CONTENUTO ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (-20).dp)
                .padding(horizontal = 16.dp)
        ) {
            // Grafico a Barre Mensili
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.balance_12_months),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- 3. PASSA IL MESE SELEZIONATO E LA CALLBACK AL GRAFICO ---
                    MonthlyBarChart(
                        data = monthlyBalances,
                        currencySymbol = currencySymbol,
                        isAmountHidden = isAmountHidden,
                        selectedMonth = selectedReportMonth,
                        onMonthSelected = { selectedReportMonth = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 4. TITOLO DINAMICO ---
            val monthName = monthToShow.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            Text(
                stringResource(R.string.category_detail_current_month, monthName), // Stringa che accetta un parametro
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            // Lista Spese per Categoria
            if (expenseByCategory.isEmpty()) {
                Text(
                    text = "Nessuna spesa registrata in questo mese.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    expenseByCategory.forEach { (categoryId, amount) ->
                        val category = categories.firstOrNull { it.id == categoryId }
                            ?: categories.firstOrNull { it.id == "other" }

                        val percentage = if (totalMonthlyExpense > 0) (amount / totalMonthlyExpense).toFloat() else 0f

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = category?.icon ?: "❓", style = MaterialTheme.typography.titleMedium)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        category?.let { getLocalizedCategoryLabel(it) } ?: stringResource(R.string.cat_other),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(Locale.getDefault(), "%.2f", amount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LinearProgressIndicator(
                                        progress = { percentage },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        strokeCap = StrokeCap.Round,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.0f%%", percentage * 100),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyBarChart(
    data: List<Pair<YearMonth, Double>>,
    currencySymbol: String,
    isAmountHidden: Boolean,
    selectedMonth: YearMonth?,
    onMonthSelected: (YearMonth) -> Unit
) {
    if (data.isEmpty()) return

    val maxAbs = data.maxOfOrNull { kotlin.math.abs(it.second) }?.toFloat()?.coerceAtLeast(1f) ?: 1f

    // MODIFICA: Box padre che contiene il grafico E il tooltip (non più Popup)
    Box(modifier = Modifier.fillMaxWidth()) {
        // Il grafico
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { (month, balance) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onMonthSelected(month) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // PARTE POSITIVA
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (balance > 0) {
                                val heightFraction = (balance.toFloat() / maxAbs).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(heightFraction)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            if (selectedMonth == month) Color(0xFF2E7D32) else Color(0xFF43A047)
                                        )
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                        // PARTE NEGATIVA
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (balance < 0) {
                                val heightFraction = (kotlin.math.abs(balance).toFloat() / maxAbs).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(heightFraction)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                                        .background(
                                            if (selectedMonth == month) Color(0xFFB71C1C) else MaterialTheme.colorScheme.error
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = month.month.getDisplayName(TextStyle.NARROW, Locale.getDefault()).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selectedMonth == month) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = 10.sp,
                        color = if (selectedMonth == month) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        // TOOLTIP EMBEDDED (non Popup)
        // Lo mostriamo sopra il grafico usando l'allineamento del Box
        // AnimatedVisibility per un effetto più fluido
        AnimatedVisibility(
            visible = selectedMonth != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            selectedMonth?.let { month ->
                val balance = data.find { it.first == month }?.second ?: 0.0
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    // Un po' di margine superiore se necessario, o padding interno
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.inverseOnSurface
                        )
                        Text(
                            text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(Locale.getDefault(), "%.2f", balance)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if(balance >= 0) Color(0xFF66BB6A) else Color(0xFFEF5350)
                        )
                    }
                }
            }
        }
    }
}
