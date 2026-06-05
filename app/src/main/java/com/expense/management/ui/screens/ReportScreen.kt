package com.expense.management.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.ReportData
import com.expense.management.ui.screens.report.CategoryTransactionsBottomSheetContent
import com.expense.management.ui.screens.report.MonthSelector
import com.expense.management.ui.screens.report.MonthlyBarChart
import com.expense.management.ui.screens.report.ReportSummaryCard
import com.expense.management.ui.theme.AppStyle
import com.expense.management.ui.theme.AppTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.text.intl.Locale as ComposeLocale

private fun String.capitalizeFirstLetter(locale: java.util.Locale = Locale.getDefault()): String {
    return replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

private val formatterCache = java.util.concurrent.ConcurrentHashMap<String, DateTimeFormatter>()

private fun parseDateSafe(dateString: String, dateFormat: String): LocalDate {
    val cachedFormatter = formatterCache.getOrPut(dateFormat) { DateTimeFormatter.ofPattern(dateFormat) }
    return try {
        LocalDate.parse(dateString, cachedFormatter)
    } catch (_: Exception) {
        try {
            LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
            LocalDate.now()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    modifier: Modifier = Modifier,
    reportData: ReportData,
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    dateFormat: String,
    isAmountHidden: Boolean,
    isLoading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    allPaymentMethods: List<PaymentMethodEntity> = emptyList(),
    onRangeChanged: (YearMonth, YearMonth) -> Unit = { _, _ -> },
) {
    val data = reportData
    val savings = data.savings
    val totalIncome = data.totalIncome
    val totalExpense = data.totalExpense
    val expenseByCategory = data.expenseByCategory
    val totalMonthlyExpense = data.totalMonthlyExpense
    val monthlyBalances = data.monthlyBalances.map { it.month to it.balance }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (error != null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.Warning, contentDescription = stringResource(R.string.error), modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Text(error, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
        return
    }

    val locale = ComposeLocale.current.platformLocale
    var selectedReportMonth by remember { mutableStateOf<YearMonth?>(YearMonth.now()) }
    var reportStartMonth by remember { mutableStateOf(YearMonth.now().minusMonths(2)) }
    var reportEndMonth by remember { mutableStateOf(YearMonth.now()) }

    var selectedCategoryIdForDetails by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(reportStartMonth, reportEndMonth) {
        selectedReportMonth = when {
            selectedReportMonth == null -> reportEndMonth
            selectedReportMonth!!.isBefore(reportStartMonth) -> reportStartMonth
            selectedReportMonth!!.isAfter(reportEndMonth) -> reportEndMonth
            else -> selectedReportMonth
        }
        onRangeChanged(reportStartMonth, reportEndMonth)
    }

    val scrollState = rememberScrollState()

    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(24.dp),
        ) {
            Text(
                stringResource(R.string.report_year, reportEndMonth.year),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        stringResource(R.string.total_savings),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                    Text(
                        text = if (isAmountHidden) {
                            "$currencySymbol *****"
                        } else {
                            "$currencySymbol ${String.format(locale, "%.2f", savings)}"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (savings >= 0) {
                        Icons.AutoMirrored.Filled.TrendingUp
                    } else {
                        Icons.AutoMirrored.Filled.TrendingDown
                    },
                    contentDescription = stringResource(R.string.savings_trend),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.filter_report_by_month),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
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
                            reportStartMonth = newStartMonth
                        },
                        label = stringResource(R.string.start_month),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    MonthSelector(
                        selectedMonth = reportEndMonth,
                        onMonthSelected = { newEndMonth ->
                            if (newEndMonth.isBefore(reportStartMonth)) {
                                reportStartMonth = newEndMonth
                            }
                            reportEndMonth = newEndMonth
                        },
                        label = stringResource(R.string.end_month),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReportSummaryCard(
                title = stringResource(R.string.income),
                amount = totalIncome,
                currencySymbol = currencySymbol,
                isAmountHidden = isAmountHidden,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            ReportSummaryCard(
                title = stringResource(R.string.expenses),
                amount = totalExpense,
                currencySymbol = currencySymbol,
                isAmountHidden = isAmountHidden,
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(R.string.balance_report),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = stringResource(R.string.bar_chart),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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

        val fullMonthFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }
        val shortMonthFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM yyyy", locale) }
        val rangeLabel = if (reportStartMonth == reportEndMonth) {
            reportStartMonth.format(fullMonthFormatter).capitalizeFirstLetter(locale)
        } else {
            val start = reportStartMonth.format(shortMonthFormatter).capitalizeFirstLetter(locale)
            val end = reportEndMonth.format(shortMonthFormatter).capitalizeFirstLetter(locale)
            "$start - $end"
        }

        Text(
            stringResource(R.string.category_detail_current_month, rangeLabel),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
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
                            contentDescription = stringResource(R.string.no_data),
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_expenses_for_month),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    expenseByCategory.forEachIndexed { index, summary ->
                        AnimatedVisibility(
                            visibleState = visibleState,
                            enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = index * 50)) +
                                slideInVertically(animationSpec = tween(durationMillis = 500, delayMillis = index * 50)) { 50 },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCategoryIdForDetails = summary.categoryId },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = summary.categoryIcon,
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            summary.categoryName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(locale, "%.2f", summary.amount)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        LinearProgressIndicator(
                                            progress = { summary.percentage },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            strokeCap = StrokeCap.Round,
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = String.format(locale, "%.0f%%", summary.percentage * 100),
                                            style = MaterialTheme.typography.labelSmall,
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
            Spacer(modifier = Modifier.height(80.dp))
        }

        if (selectedCategoryIdForDetails != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedCategoryIdForDetails = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = true) },
            ) {
                val categoryId = selectedCategoryIdForDetails!!
                val categoryName = reportData.expenseByCategory.find { it.categoryId == categoryId }?.categoryName
                    ?: stringResource(R.string.cat_other)
                val transactionsForSelectedCategory = remember(transactions, reportStartMonth, reportEndMonth, categoryId) {
                    transactions.filter {
                        it.type == TransactionType.EXPENSE &&
                            it.categoryId == categoryId && try {
                                val m = YearMonth.from(LocalDate.parse(it.effectiveDate))
                                !m.isBefore(reportStartMonth) && !m.isAfter(reportEndMonth)
                            } catch (e: Exception) {
                                false
                            }
                    }.sortedByDescending { parseDateSafe(it.date, dateFormat) }
                }

                CategoryTransactionsBottomSheetContent(
                    transactionsForCategory = transactionsForSelectedCategory,
                    categoryName = categoryName,
                    currencySymbol = currencySymbol,
                    isAmountHidden = isAmountHidden,
                    dateFormat = dateFormat,
                    allPaymentMethods = allPaymentMethods,
                )
            }
        }
    }
}

@Composable
private fun ReportPreview() {
    AppTheme(appStyle = AppStyle.MATERIAL_YOU, darkTheme = false) {
        ReportScreen(reportData = ReportData.EMPTY, transactions = emptyList(), currencySymbol = "\u20AC", dateFormat = "dd/MM/yyyy", isAmountHidden = false)
    }
}

@Preview(showBackground = true, name = "Report Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReportPreviewDark() {
    AppTheme(appStyle = AppStyle.MATERIAL_YOU, darkTheme = true) {
        ReportScreen(reportData = ReportData.EMPTY, transactions = emptyList(), currencySymbol = "\u20AC", dateFormat = "dd/MM/yyyy", isAmountHidden = false)
    }
}
