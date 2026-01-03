package com.expense.management.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.expense.management.R
import com.expense.management.data.CategoryEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.utils.getLocalizedCategoryLabel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private fun String.capitalizeFirstLetter(): String {
    return replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

private fun parseDateSafe(dateString: String, dateFormat: String): LocalDate {
    val formatters = setOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern(dateFormat),
    )
    for (formatter in formatters) {
        try {
            return LocalDate.parse(dateString, formatter)
        } catch (e: Exception) {
            // ignore and try next
        }
    }
    // Fallback to avoid crash
    return LocalDate.now()
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var reportStartMonth by remember { mutableStateOf(YearMonth.now().minusMonths(11)) }
    var reportEndMonth by remember { mutableStateOf(YearMonth.now()) }

    // --- STATO PER I DETTAGLI DELLE TRANSAZIONI PER CATEGORIA ---
    var selectedCategoryIdForDetails by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reportStartMonth, reportEndMonth) {
        selectedReportMonth = when {
            selectedReportMonth == null -> reportEndMonth
            selectedReportMonth!!.isBefore(reportStartMonth) -> reportStartMonth
            selectedReportMonth!!.isAfter(reportEndMonth) -> reportEndMonth
            else -> selectedReportMonth
        }
    }

    // Calcolo Risparmio Anno Corrente
    val savings = remember(transactions, reportStartMonth, reportEndMonth) {
        transactions
            .filter { transaction ->
                try {
                    val transactionMonth = YearMonth.from(parseDateSafe(transaction.effectiveDate, dateFormat))
                    !transactionMonth.isBefore(reportStartMonth) && !transactionMonth.isAfter(reportEndMonth)
                } catch (e: Exception) {
                    false
                }
            }
            .sumOf { if (it.type == TransactionType.INCOME) it.amount else -it.amount }
    }

    // --- 2. CALCOLO SPESE PER CATEGORIA (DINAMICO) ---
    val monthToShow = selectedReportMonth ?: reportEndMonth

    val expenseByCategory = remember(transactions, monthToShow) {
        transactions
            .filter {
                it.type == TransactionType.EXPENSE && try {
                    YearMonth.from(parseDateSafe(it.effectiveDate, dateFormat)) == monthToShow
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

    // Calcolo Bilancio Mensile (Range Selezionato)
    val monthlyBalances = remember(transactions, reportStartMonth, reportEndMonth) {
        val balances = mutableListOf<Pair<YearMonth, Double>>()
        var current = reportStartMonth
        while (!current.isAfter(reportEndMonth)) {
            val monthlyTransactions = transactions.filter { transaction ->
                try {
                    YearMonth.from(parseDateSafe(transaction.effectiveDate, dateFormat)) == current
                } catch (e: Exception) {
                    false
                }
            }
            val income = monthlyTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = monthlyTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            balances.add(current to (income - expense))
            current = current.plusMonths(1)
        }
        balances
    }

    val scrollState = rememberScrollState()

    // Animation state
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // AGGIUNTO QUI: Rende l'intera schermata scrollabile
            .verticalScroll(scrollState),
    ) {
        // --- HEADER ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ),
                )
                .padding(24.dp)
                // More space for the card overlap
                .padding(bottom = 32.dp),
        ) {
            Text(
                stringResource(R.string.report_year, reportEndMonth.year),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        stringResource(R.string.total_savings),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                    Text(
                        text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(Locale.getDefault(), "%.2f", savings)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (savings >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    tint = Color.White,
                )
            }
        }

        // --- MONTH FILTERS CARD ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-48).dp)
                .zIndex(2f),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.filter_report_by_month),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonthSelector(
                        selectedMonth = reportStartMonth,
                        onMonthSelected = { newStartMonth ->
                            if (newStartMonth.isAfter(reportEndMonth)) {
                                reportEndMonth = newStartMonth
                            }
                            if (newStartMonth.plusMonths(11).isBefore(reportEndMonth)) {
                                reportEndMonth = newStartMonth.plusMonths(11)
                            }
                            reportStartMonth = newStartMonth
                        },
                        label = stringResource(R.string.start_month),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    MonthSelector(
                        selectedMonth = reportEndMonth,
                        onMonthSelected = { newEndMonth ->
                            if (newEndMonth.isBefore(reportStartMonth)) {
                                reportStartMonth = newEndMonth
                            }
                            if (newEndMonth.minusMonths(11).isAfter(reportStartMonth)) {
                                reportStartMonth = newEndMonth.minusMonths(11)
                            }
                            reportEndMonth = newEndMonth
                        },
                        label = stringResource(R.string.end_month),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // --- SCROLLABLE CONTENT ---
        Column(
            modifier = Modifier
                // CAMBIATO: da fillMaxSize a fillMaxWidth
                .fillMaxWidth()
                // Adjust offset to account for the new card and previous offset
                .offset(y = (-48).dp)
                // RIMOSSO: .verticalScroll(scrollState) da qui
                // Apply horizontal padding here
                .padding(horizontal = 16.dp),
        ) {
            // Spacer to visually separate the filter card from the chart card
            Spacer(modifier = Modifier.height(24.dp))

            // Grafico a Barre Mensili
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.balance_report),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- 3. GRAFICO ---
                    MonthlyBarChart(
                        data = monthlyBalances,
                        currencySymbol = currencySymbol,
                        isAmountHidden = isAmountHidden,
                        selectedMonth = selectedReportMonth,
                        onMonthSelected = { selectedReportMonth = it },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 4. TITOLO DINAMICO ---
            val monthName = monthToShow.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())).capitalizeFirstLetter()

            Text(
                stringResource(R.string.category_detail_current_month, monthName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp, start = 4.dp),
            )

            // Lista Spese per Categoria
            if (expenseByCategory.isEmpty()) {
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = fadeIn(animationSpec = tween(600)) + slideInVertically(initialOffsetY = { it / 2 }),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nessuna spesa\nregistrata per questo mese",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    expenseByCategory.forEachIndexed { index, (categoryId, amount) ->
                        AnimatedVisibility(
                            visibleState = visibleState,
                            enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = index * 50)) +
                                slideInVertically(animationSpec = tween(durationMillis = 500, delayMillis = index * 50)) { 50 },
                        ) {
                            val category = categories.firstOrNull { it.id == categoryId }
                                ?: categories.firstOrNull { it.id == "other" }

                            val percentage = if (totalMonthlyExpense > 0) (amount / totalMonthlyExpense).toFloat() else 0f

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // ADDED CLICKABLE HERE
                                    .clickable { selectedCategoryIdForDetails = categoryId },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = category?.icon ?: "🏷️",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            category?.let { getLocalizedCategoryLabel(it) } ?: stringResource(R.string.cat_other),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(Locale.getDefault(), "%.2f", amount)}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        LinearProgressIndicator(
                                            // Changed here
                                            progress = { percentage },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            strokeCap = StrokeCap.Round,
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = String.format(Locale.getDefault(), "%.0f%%", percentage * 100),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Bottom padding for FAB or Nav
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // --- Dettaglio Transazioni per Categoria ---
    AnimatedVisibility(
        visible = selectedCategoryIdForDetails != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        selectedCategoryIdForDetails?.let { categoryId ->
            val category = categories.firstOrNull { it.id == categoryId }
            val transactionsForSelectedCategory = remember(transactions, monthToShow, categoryId) {
                transactions.filter {
                    it.type == TransactionType.EXPENSE &&
                        YearMonth.from(parseDateSafe(it.effectiveDate, dateFormat)) == monthToShow &&
                        it.categoryId == categoryId
                }.sortedByDescending { parseDateSafe(it.date, dateFormat) }
            }

            CategoryTransactionsDetail(
                transactionsForCategory = transactionsForSelectedCategory,
                category = category,
                currencySymbol = currencySymbol,
                isAmountHidden = isAmountHidden,
                dateFormat = dateFormat,
                onDismiss = { selectedCategoryIdForDetails = null },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthSelector(
    selectedMonth: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val months = remember {
        (-24..0).map { YearMonth.now().plusMonths(it.toLong()) }.sortedByDescending { it }
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val responsiveFontSize = (screenWidth * 0.032f).sp

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())).capitalizeFirstLetter(),
            onValueChange = { /* Read Only */ },
            readOnly = true,
            maxLines = 1,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = responsiveFontSize),

            label = { Text(text = label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            months.forEach { month ->
                DropdownMenuItem(
                    text = {
                        Text(
                            month.format(
                                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()),
                            ).capitalizeFirstLetter(),
                            fontSize = responsiveFontSize,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                        )
                    },
                    onClick = {
                        onMonthSelected(month)
                        expanded = false
                    },
                )
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
    onMonthSelected: (YearMonth) -> Unit,
) {
    if (data.isEmpty()) return

    val maxAbs = data.maxOfOrNull { kotlin.math.abs(it.second) }?.toFloat()?.coerceAtLeast(1f) ?: 1f
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    // BoxWithConstraints per calcolare la posizione orizzontale del tooltip
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val totalWidth = maxWidth
        val barWidth = totalWidth / data.size

        // Il grafico
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            data.forEach { (month, balance) ->
                val isSelected = selectedMonth == month

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onMonthSelected(month) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        // PARTE POSITIVA
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                // Slightly thinner bars
                                .padding(horizontal = 3.dp),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            if (balance > 0) {
                                val heightFraction = (balance.toFloat() / maxAbs).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(heightFraction)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                            },
                                        ),
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp,
                        )

                        // PARTE NEGATIVA
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 3.dp),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            if (balance < 0) {
                                val heightFraction = (kotlin.math.abs(balance).toFloat() / maxAbs).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(heightFraction)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                                        .background(
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                                            },
                                        ),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = month.month.getDisplayName(TextStyle.NARROW, Locale.getDefault()).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                        fontSize = 11.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }

        // TOOLTIP POSIZIONATO DINAMICAMENTE
        selectedMonth?.let { month ->
            val index = data.indexOfFirst { it.first == month }
            if (index >= 0) {
                val xOffset = (barWidth * index) + (barWidth / 2)
                // Stima approssimativa della larghezza del tooltip
                val tooltipWidth = 80.dp

                // Calcola se il tooltip sta per uscire dai bordi
                val currentX = xOffset
                val minX = tooltipWidth / 2
                val maxX = totalWidth - (tooltipWidth / 2)

                // Applica un offset correttivo per mantenerlo nei limiti
                val extraOffset = when {
                    index <= 9 -> (-16).dp
                    index >= 10 -> (-40).dp
                    else -> 0.dp
                }

                Box(
                    modifier = Modifier
                        .absoluteOffset(x = xOffset + extraOffset, y = 0.dp)
                        .zIndex(1f),
                    // Align to bottom of this anchor point (effectively top of chart)
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    // We want the tooltip to appear *above* the chart area if possible,
                    // but BoxWithConstraints starts at top of chart area.
                    // So y=0 is the top line of the 200dp chart.

                    // Use a negative offset to push it up.
                    Box(modifier = Modifier.offset(y = (-48).dp)) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            val balance = data.find { it.first == month }?.second ?: 0.0
                            val isPositive = balance >= 0

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                ),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(6.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = if (isAmountHidden) "*****" else "${String.format(Locale.getDefault(), "%.2f", balance)} $currencySymbol",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTransactionsDetail(
    transactionsForCategory: List<TransactionEntity>,
    category: CategoryEntity?,
    currencySymbol: String,
    isAmountHidden: Boolean,
    dateFormat: String,
    onDismiss: () -> Unit,
) {
    val categoryName = category?.let { getLocalizedCategoryLabel(it) } ?: stringResource(R.string.cat_other)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.transactions_for_category, categoryName)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) { paddingValues ->
        if (transactionsForCategory.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.no_transactions_for_category, categoryName),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(transactionsForCategory) { transaction ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = transaction.description.ifEmpty { stringResource(R.string.no_description) },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = LocalDate.parse(transaction.date, DateTimeFormatter.ISO_LOCAL_DATE)
                                        .format(DateTimeFormatter.ofPattern(dateFormat, Locale.getDefault())),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(Locale.getDefault(), "%.2f", transaction.amount)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (transaction.type == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}
