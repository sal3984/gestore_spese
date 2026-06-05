package com.expense.management.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.data.CategoryEntity
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.BnplProjection
import com.expense.management.domain.model.CreditCardSummary
import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.DeleteType
import com.expense.management.ui.model.TransactionToDelete
import com.expense.management.ui.theme.gestoreSpeseTheme
import com.expense.management.utils.TransactionItem
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.text.intl.Locale as ComposeLocale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    dateFormat: String,
    earliestMonth: YearMonth,
    currentDashboardMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onDelete: (String, DeleteType) -> Unit,
    onEdit: (String, Boolean) -> Unit,
    isAmountHidden: Boolean,
    creditCards: List<ActiveCreditCard> = emptyList(),
    bnplProjections: List<BnplProjection> = emptyList(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    isLoading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    allPaymentMethods: List<PaymentMethodEntity> = emptyList(),
    enabledWidgets: Set<com.expense.management.domain.model.DashboardWidget> = com.expense.management.domain.model.DashboardWidget.entries.toSet(),
    dashboardFilteredTransactions: List<TransactionEntity> = emptyList(),
    creditCardSummaries: Map<String, CreditCardSummary> = emptyMap(),
    onPayRevolving: (ActiveCreditCard, Double, LocalDate) -> Unit = { _, _, _ -> },
) {
    val today = YearMonth.now()
    val locale = ComposeLocale.current.platformLocale
    val monthFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }

    val currentTrans = dashboardFilteredTransactions

    val groupedTransactions = remember(currentTrans) {
        currentTrans.groupBy { it.effectiveDate }
    }

    val totalIncome = remember(currentTrans) { currentTrans.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } }
    val totalExpense = remember(currentTrans) { currentTrans.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount } }
    val netBalance = remember(totalIncome, totalExpense) { totalIncome - totalExpense }

    // LIMITI NAVIGAZIONE
    val minMonth = if (earliestMonth.isBefore(today.minusMonths(3))) earliestMonth else today.minusMonths(3)
    val maxMonth = today.plusMonths(12)

    // State for entrance animation
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    var payDialogCard by remember { mutableStateOf<ActiveCreditCard?>(null) }

    var showDeleteDialog by remember { mutableStateOf<TransactionToDelete?>(null) }

    if (showDeleteDialog != null) {
        val transactionToDelete = showDeleteDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.delete_transaction_title)) },
            text = {
                if (transactionToDelete.isInstallment) {
                    Text(stringResource(R.string.delete_installment_message))
                } else {
                    Text(stringResource(R.string.delete_transaction_message))
                }
            },
            confirmButton = {
                if (transactionToDelete.isInstallment) {
                    Column {
                        TextButton(
                            onClick = {
                                onDelete(transactionToDelete.transaction.id, DeleteType.THIS_AND_SUBSEQUENT)
                                showDeleteDialog = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(stringResource(R.string.delete_this_and_subsequent))
                        }
                        TextButton(
                            onClick = {
                                onDelete(transactionToDelete.transaction.id, DeleteType.SINGLE)
                                showDeleteDialog = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(stringResource(R.string.delete_single_installment))
                        }
                        // Spostato il pulsante Annulla qui per allineamento
                        TextButton(onClick = { showDeleteDialog = null }) {
                            Text(stringResource(R.string.cancel).uppercase())
                        }
                    }
                } else {
                    TextButton(
                        onClick = {
                            onDelete(transactionToDelete.transaction.id, DeleteType.SINGLE)
                            showDeleteDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(R.string.delete_uppercase))
                    }
                }
            },
            dismissButton = { /* Vuoto, il pulsante Annulla è ora nel confirmButton's Column */ },
        )
    }

    // Unica LazyColumn per permettere lo scroll di tutta la pagina (anche header)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp),
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
                                    MaterialTheme.colorScheme.primaryContainer,
                                ),
                            ),
                        )
                        .padding(bottom = 48.dp),
                ) {
                    // Navigazione Mese
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, start = 8.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { onMonthChange(currentDashboardMonth.minusMonths(1)) },
                            enabled = currentDashboardMonth > minMonth,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.previous_month),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }

                        Text(
                            currentDashboardMonth.format(monthFormatter).replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )

                        IconButton(
                            onClick = { onMonthChange(currentDashboardMonth.plusMonths(1)) },
                            enabled = currentDashboardMonth < maxMonth,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.next_month),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Saldo Centrale
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            stringResource(R.string.monthly_balance),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        )
                        Text(
                            text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(locale, "%.2f", netBalance)}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- CARDS ---
                val showSummary = com.expense.management.domain.model.DashboardWidget.SUMMARY_CARDS in enabledWidgets
                val showBnpl = com.expense.management.domain.model.DashboardWidget.BNPL_PROJECTIONS in enabledWidgets
                val showCreditCards = com.expense.management.domain.model.DashboardWidget.CREDIT_CARD_INFO in enabledWidgets
                if (showSummary || showBnpl || showCreditCards) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-40).dp)
                            .padding(horizontal = 16.dp),
                    ) {
                        if (showSummary) {
                            // Card Entrate/Uscite
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    // Entrate
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondaryContainer),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                Icons.Default.ArrowUpward,
                                                stringResource(R.string.income),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                stringResource(R.string.income),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(locale, "%.2f", totalIncome)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary,
                                            )
                                        }
                                    }

                                    // Uscite
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                stringResource(R.string.expenses),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(locale, "%.2f", totalExpense)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.errorContainer),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                Icons.Default.ArrowDownward,
                                                stringResource(R.string.expenses),
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Card Debiti Previsti BNPL
                        if (showBnpl && bnplProjections.isNotEmpty()) {
                            val totalBnplDebt = bnplProjections.sumOf { it.totalExpected }
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Payment,
                                            contentDescription = stringResource(R.string.bnpl_debt),
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.bnpl_debt),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(locale, "%.2f", totalBnplDebt)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                    bnplProjections.forEach { proj ->
                                        Text(
                                            text = "${proj.methodName}: ${proj.installments.size} rate",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                        )
                                    }
                                }
                            }
                        }

                        // Card Carte di Credito
                        if (showCreditCards && creditCards.isNotEmpty()) {
                            val pagerState = rememberPagerState(pageCount = { creditCards.size })
                            HorizontalPager(
                                state = pagerState,
                                pageSpacing = 16.dp,
                                contentPadding = PaddingValues(horizontal = if (creditCards.size > 1) 32.dp else 0.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) { page ->
                                val card = creditCards[page]
                                val summary = creditCardSummaries[card.id]

                                val displayedSpent = summary?.displayedSpent ?: 0.0
                                val totalUtilizedForDisplay = summary?.totalUtilized ?: 0.0
                                val totalPaidForDisplay = summary?.totalPaid ?: 0.0
                                val progress = summary?.progress ?: 0f

                                CreditCardItem(
                                    name = card.name,
                                    limit = card.limit,
                                    spent = displayedSpent,
                                    progress = progress,
                                    currencySymbol = currencySymbol,
                                    isAmountHidden = isAmountHidden,
                                    type = card.cardType,
                                    totalUtilized = totalUtilizedForDisplay,
                                    totalPaid = totalPaidForDisplay,
                                    locale = locale,
                                    totalRepaid = summary?.totalRepaid ?: 0.0,
                                    onPayInstallment = if (card.cardType == CreditCardType.REVOLVING) {
                                        { payDialogCard = card }
                                    } else {
                                        null
                                    },
                                )
                            }

                            if (creditCards.size > 1) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    repeat(pagerState.pageCount) { iteration ->
                                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        Box(
                                            modifier = Modifier
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .size(8.dp),
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Revolving Payment Dialog
                        payDialogCard?.let { card ->
                            CreditCardPaymentDialog(
                                card = card,
                                summary = creditCardSummaries[card.id],
                                currencySymbol = currencySymbol,
                                isAmountHidden = isAmountHidden,
                                locale = locale,
                                onDismiss = { payDialogCard = null },
                                onConfirm = { paymentAmount, paymentDate ->
                                    onPayRevolving(card, paymentAmount, paymentDate)
                                    payDialogCard = null
                                },
                            )
                        }
                    }
                }
            }
        }

        // Lista Transazioni
        if (com.expense.management.domain.model.DashboardWidget.TRANSACTION_LIST in enabledWidgets) {
            if (groupedTransactions.isEmpty()) {
                item {
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = fadeIn(animationSpec = tween(durationMillis = 600)) + slideInVertically(initialOffsetY = { it / 2 }),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = stringResource(R.string.report_no_transaction_this_month),
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.report_no_transaction_this_month),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.report_message_add_transaction),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                groupedTransactions.forEach { (dateString, transactionsOnDate) ->
                    val dailyTotal = transactionsOnDate.sumOf { t ->
                        if (t.type == TransactionType.INCOME) t.amount else -t.amount
                    }

                    stickyHeader {
                        DateHeader(Modifier, dateString, dailyTotal, currencySymbol, isAmountHidden, locale)
                    }

                    items(transactionsOnDate, key = { it.id }) { t ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    showDeleteDialog = TransactionToDelete(
                                        transaction = t,
                                        isInstallment = t.installmentNumber != null && t.totalInstallments != null && t.totalInstallments > 1,
                                    )
                                    false
                                } else {
                                    false
                                }
                            },
                        )

                        AnimatedVisibility(
                            visibleState = visibleState,
                            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(initialOffsetY = { 50 }),
                            modifier = Modifier.animateItem(),
                        ) {
                            SwipeToDismissBox(
                                state = dismissState,
                                modifier = Modifier
                                    .padding(vertical = 4.dp, horizontal = 16.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = true,
                                backgroundContent = {
                                    val color = when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        else -> Color.Transparent
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(color)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.delete),
                                            tint = MaterialTheme.colorScheme.onError,
                                        )
                                    }
                                },
                                content = {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = RoundedCornerShape(16.dp),
                                        tonalElevation = 2.dp,
                                        shadowElevation = 1.dp,
                                    ) {
                                        TransactionItem(
                                            transaction = t,
                                            categories = categories,
                                            currencySymbol = currencySymbol,
                                            dateFormat = dateFormat,
                                            isAmountHidden = isAmountHidden,
                                            onDelete = { /* Gestito da SwipeToDismissBox */ },
                                            onEdit = onEdit,
                                            locale = locale,
                                            sharedTransitionScope = sharedTransitionScope,
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            allPaymentMethods = allPaymentMethods,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreditCardItem(
    modifier: Modifier = Modifier,
    name: String,
    limit: Double,
    spent: Double,
    progress: Float,
    currencySymbol: String,
    isAmountHidden: Boolean,
    type: CreditCardType,
    totalUtilized: Double = 0.0,
    totalPaid: Double = 0.0,
    totalRepaid: Double = 0.0,
    locale: Locale = Locale.getDefault(),
    onPayInstallment: (() -> Unit)? = null,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CreditCard, stringResource(R.string.credit_card), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (type == CreditCardType.REVOLVING) {
                        Text(
                            stringResource(R.string.installment_plan),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (progress > 0.9f) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = stringResource(R.string.limit_warning),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (type == CreditCardType.REVOLVING) {
                Text(
                    text = if (type == CreditCardType.REVOLVING) stringResource(R.string.revolving_utilized_label) else stringResource(R.string.spent_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(locale, "%.2f", totalUtilized)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.revolving_paid_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(locale, "%.2f", totalRepaid)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (progress > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (isAmountHidden) "${if (type == CreditCardType.REVOLVING) stringResource(R.string.revolving_remaining_label) else stringResource(R.string.spent_label)} $currencySymbol *****" else "${if (type == CreditCardType.REVOLVING) stringResource(R.string.revolving_remaining_label) else stringResource(R.string.spent_label)} $currencySymbol ${String.format(locale, "%.2f", spent)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${stringResource(R.string.limit_label)} $currencySymbol ${String.format(locale, "%.2f", limit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onPayInstallment != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onPayInstallment,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pay_installment))
                }
            }
        }
    }
}

@Composable
fun DateHeader(
    modifier: Modifier = Modifier,
    dateString: String,
    dailyTotal: Double,
    currencySymbol: String,
    isAmountHidden: Boolean,
    locale: Locale = Locale.getDefault(),
) {
    val date = try {
        LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: Exception) {
        LocalDate.now()
    }

    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    val label = when (date) {
        today -> stringResource(R.string.today)
        yesterday -> stringResource(R.string.yesterday)
        else -> date.format(DateTimeFormatter.ofPattern("dd MMMM", locale))
    }

    val totalColor = when {
        dailyTotal > 0 -> MaterialTheme.colorScheme.secondary
        dailyTotal < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Text(
                text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(locale, "%.2f", dailyTotal)}",
                style = MaterialTheme.typography.labelSmall,
                color = totalColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun CreditCardPaymentDialog(
    card: ActiveCreditCard,
    summary: CreditCardSummary?,
    currencySymbol: String,
    isAmountHidden: Boolean,
    locale: Locale,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, date: LocalDate) -> Unit,
) {
    val isRevolving = card.cardType == CreditCardType.REVOLVING
    val outstanding = summary?.displayedSpent ?: 0.0
    var amountText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isRevolving) "Paga Rata ${card.name}" else "Paga Estratto Conto ${card.name}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Plafond: $currencySymbol ${String.format(locale, "%.2f", card.limit)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Residuo: $currencySymbol ${if (isAmountHidden) "*****" else String.format(locale, "%.2f", outstanding)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Importo rata") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Data pagamento") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, "Seleziona data")
                        }
                    },
                )
                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    dateText = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                                }
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Annulla") }
                        },
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@TextButton
                    val date = try {
                        LocalDate.parse(dateText, DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (_: Exception) {
                        return@TextButton
                    }
                    onConfirm(amount, date)
                },
                enabled = amountText.toDoubleOrNull() != null && amountText.toDoubleOrNull()!! > 0,
            ) { Text("Paga") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}

@Preview(showBackground = true, name = "Dashboard Light")
@Composable
private fun DashboardPreview() {
    gestoreSpeseTheme(darkTheme = false, dynamicColor = false) {
        DashboardScreen(transactions = emptyList(), categories = emptyList(), currencySymbol = "€", dateFormat = "dd/MM/yyyy", earliestMonth = java.time.YearMonth.now(), currentDashboardMonth = java.time.YearMonth.now(), onMonthChange = {}, onDelete = { _, _ -> }, onEdit = { _, _ -> }, isAmountHidden = false)
    }
}

@Preview(showBackground = true, name = "Dashboard Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardPreviewDark() {
    gestoreSpeseTheme(darkTheme = true, dynamicColor = false) {
        DashboardScreen(transactions = emptyList(), categories = emptyList(), currencySymbol = "€", dateFormat = "dd/MM/yyyy", earliestMonth = java.time.YearMonth.now(), currentDashboardMonth = java.time.YearMonth.now(), onMonthChange = {}, onDelete = { _, _ -> }, onEdit = { _, _ -> }, isAmountHidden = false)
    }
}
