package com.expense.management.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
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

    // Helper per parsing sicuro delle date (ISO o fallback)
    fun parseDateSafe(dateString: String): LocalDate {
        return try {
            LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            try {
                // Fallback generico o tentativo di usare il formato utente corrente se coincide
                LocalDate.parse(dateString, DateTimeFormatter.ofPattern(dateFormat))
            } catch (e2: Exception) {
                LocalDate.now() // Fallback finale per evitare crash, anche se distorce leggermente i dati
            }
        }
    }

    // Calcolo Risparmio Anno Corrente
    val currentYear = LocalDate.now().year
    val savings = transactions
        .filter {
             try {
                 parseDateSafe(it.effectiveDate).year == currentYear
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
                    YearMonth.from(parseDateSafe(it.effectiveDate)) == monthToShow
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
                    YearMonth.from(parseDateSafe(it.effectiveDate)) == targetMonth
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

    // Animation state
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

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
                .padding(bottom = 32.dp) // More space for the card overlap
        ) {
            Text(
                stringResource(R.string.report_year, currentYear),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        stringResource(R.string.total_savings),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.9f)
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
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    tint = Color.White
                )
            }
        }

        // --- CONTENUTO ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (-24).dp) // Slight overlap for better UI depth
                .padding(horizontal = 16.dp)
        ) {
            // Grafico a Barre Mensili
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.balance_12_months),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- 3. GRAFICO ---
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
                stringResource(R.string.category_detail_current_month, monthName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
            )

            // Lista Spese per Categoria
            if (expenseByCategory.isEmpty()) {
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = fadeIn(animationSpec = tween(600)) + slideInVertically(initialOffsetY = { it / 2 })
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nessuna spesa\nregistrata per questo mese",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
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
                                    slideInVertically(animationSpec = tween(durationMillis = 500, delayMillis = index * 50)) { 50 }
                        ) {
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
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = category?.icon ?: "🏷️",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            category?.let { getLocalizedCategoryLabel(it) } ?: stringResource(R.string.cat_other),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(Locale.getDefault(), "%.2f", amount)}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        LinearProgressIndicator(
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
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Bottom padding for FAB or Nav
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
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { (month, balance) ->
                val isSelected = selectedMonth == month

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
                                .padding(horizontal = 3.dp), // Slightly thinner bars
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (balance > 0) {
                                val heightFraction = (balance.toFloat() / maxAbs).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(heightFraction)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                        )
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )

                        // PARTE NEGATIVA
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 3.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (balance < 0) {
                                val heightFraction = (kotlin.math.abs(balance).toFloat() / maxAbs).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(heightFraction)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                                        )
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
                        maxLines = 1
                    )
                }
            }
        }

        // TOOLTIP POSIZIONATO DINAMICAMENTE
        selectedMonth?.let { month ->
            val index = data.indexOfFirst { it.first == month }
            if (index >= 0) {
                val xOffset = (barWidth * index) + (barWidth / 2)
                val tooltipWidth = 80.dp // Stima approssimativa della larghezza del tooltip

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
                        .absoluteOffset(x = xOffset + extraOffset, y = 0.dp) // y=0 is top of BoxWithConstraints
                        .zIndex(1f),
                    contentAlignment = Alignment.BottomCenter // Align to bottom of this anchor point (effectively top of chart)
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
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                ),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (isAmountHidden) "*****" else "${String.format(Locale.getDefault(), "%.0f", balance)} $currencySymbol",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
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
