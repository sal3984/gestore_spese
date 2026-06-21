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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity
import com.expense.management.data.AmexRevolvingStateEntity
import com.expense.management.data.AmexStatementEntity
import com.expense.management.data.CategoryEntity
import com.expense.management.data.CreditCardInstallmentPlanEntity
import com.expense.management.data.InstallmentScheduledPaymentEntity
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.data.TransactionEntity
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.ActiveCreditCard
import com.expense.management.domain.model.AmexDashboardProjection
import com.expense.management.domain.model.AmexInstallmentStrategy
import com.expense.management.domain.model.AmexPaymentMode
import com.expense.management.domain.model.BnplProjection
import com.expense.management.domain.model.CreditCardSummary
import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.DeleteType
import com.expense.management.domain.model.PaymentProvider
import com.expense.management.ui.model.TransactionToDelete
import com.expense.management.ui.screens.amex.AmexInstallmentCard
import com.expense.management.ui.screens.amex.AmexInstallmentSetupDialog
import com.expense.management.ui.theme.AppStyle
import com.expense.management.ui.theme.AppTheme
import com.expense.management.utils.TransactionItem
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
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
    installmentPlans: List<CreditCardInstallmentPlanEntity> = emptyList(),
    scheduledPayments: List<InstallmentScheduledPaymentEntity> = emptyList(),
    onPayRevolving: (ActiveCreditCard, Double, LocalDate) -> Unit = { _, _, _ -> },
    onPayInstallmentPlan: (CreditCardInstallmentPlanEntity, ActiveCreditCard, LocalDate) -> Unit = { _, _, _ -> },
    onSetupInstallmentPlan: (paymentMethodId: String, totalAmount: Double, installmentCount: Int, installmentAmount: Double, startDate: String) -> Unit = { _, _, _, _, _ -> },
    amexStatements: List<AmexStatementEntity> = emptyList(),
    amexPagoFlexPlans: List<AmexPagoFlexPlanEntity> = emptyList(),
    amexRevolvingStates: List<AmexRevolvingStateEntity> = emptyList(),
    onCreateAmexStatement: (paymentMethodId: String, statementMonth: String, closingDate: String, paymentDueDate: String) -> Unit = { _, _, _, _ -> },
    onSetAmexPaymentMode: (statementId: String, mode: AmexPaymentMode, amount: Double) -> Unit = { _, _, _ -> },
    onPayAmexStatement: (statement: AmexStatementEntity, amount: Double) -> Unit = { _, _ -> },
    amexProjections: List<AmexDashboardProjection> = emptyList(),
    isAmexAutoPayEnabled: Boolean = true,
    onToggleAmexAutoPay: (Boolean) -> Unit = {},
    amexScheduledPayments: List<AmexPagoFlexScheduledPaymentEntity> = emptyList(),
    amexCurrentAccountOutflow: Double = 0.0,
    currentAccountIncomeForMonth: Double = 0.0,
    currentAccountOutflowsForMonth: Double = 0.0,
    onEditAmexInstallment: (planId: String, strategy: AmexInstallmentStrategy) -> Unit = { _, _ -> },
) {
    val today = YearMonth.now()
    val locale = ComposeLocale.current.platformLocale
    val monthFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }

    val currentTrans = dashboardFilteredTransactions

    val groupedTransactions = remember(currentTrans) {
        currentTrans.groupBy { it.effectiveDate }
    }

    val totalIncome = currentAccountIncomeForMonth
    val totalExpense = currentAccountOutflowsForMonth
    val netBalance = totalIncome - totalExpense

    // LIMITI NAVIGAZIONE
    val minMonth = if (earliestMonth.isBefore(today.minusMonths(3))) earliestMonth else today.minusMonths(3)
    val maxMonth = today.plusMonths(12)

    // State for entrance animation
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    var payDialogCard by remember { mutableStateOf<ActiveCreditCard?>(null) }
    var installmentPayDialogCard by remember { mutableStateOf<ActiveCreditCard?>(null) }
    var installmentPayPlan by remember { mutableStateOf<CreditCardInstallmentPlanEntity?>(null) }
    var setupPlanCard by remember { mutableStateOf<ActiveCreditCard?>(null) }
    var editPlanCard by remember { mutableStateOf<ActiveCreditCard?>(null) }
    var editPlan by remember { mutableStateOf<CreditCardInstallmentPlanEntity?>(null) }

    var amexEditPlan by remember { mutableStateOf<AmexPagoFlexPlanEntity?>(null) }

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
                val showAmex = com.expense.management.domain.model.DashboardWidget.AMEX_INFO in enabledWidgets
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
                            val cardPlans = remember(installmentPlans) {
                                installmentPlans.groupBy { it.paymentMethodId }
                            }
                            val scheduledByPlan: Map<String, List<InstallmentScheduledPaymentEntity>> = remember(scheduledPayments) {
                                scheduledPayments.groupBy { it.planId }
                            }
                            HorizontalPager(
                                state = pagerState,
                                pageSpacing = 16.dp,
                                contentPadding = PaddingValues(horizontal = if (creditCards.size > 1) 32.dp else 0.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) { page ->
                                val card = creditCards[page]
                                val summary = creditCardSummaries[card.id]
                                val plansForCard = cardPlans[card.id].orEmpty()
                                val totalPlans = plansForCard.size
                                val totalInstallments = plansForCard.sumOf { it.installmentCount }
                                val totalPaidInstallments = plansForCard.sumOf { it.paidCount }
                                val unpaidPlans = plansForCard.filter { it.paidCount < it.installmentCount }
                                val nextPlan = unpaidPlans.minByOrNull { it.startDate }

                                val displayedSpent = summary?.displayedSpent ?: 0.0
                                val totalUtilizedForDisplay = summary?.totalUtilized ?: 0.0
                                val totalPaidForDisplay = summary?.totalPaid ?: 0.0
                                val progress = summary?.progress ?: 0f

                                val payAction: (() -> Unit)? = when (card.cardType) {
                                    CreditCardType.REVOLVING -> {
                                        { payDialogCard = card }
                                    }
                                    CreditCardType.INSTALLMENT -> {
                                        if (nextPlan != null) {
                                            {
                                                installmentPayPlan = nextPlan
                                                installmentPayDialogCard = card
                                            }
                                        } else {
                                            null
                                        }
                                    }
                                    else -> null
                                }
                                val setupAction: (() -> Unit)? = when (card.cardType) {
                                    CreditCardType.INSTALLMENT -> {
                                        if (nextPlan != null) {
                                            {
                                                editPlanCard = card
                                                editPlan = nextPlan
                                            }
                                        } else {
                                            {
                                                setupPlanCard = card
                                            }
                                        }
                                    }
                                    else -> null
                                }
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
                                    installmentInfo = if (card.cardType == CreditCardType.INSTALLMENT && totalPlans > 0) {
                                        "$totalPaidInstallments/$totalInstallments rate pagate"
                                    } else {
                                        null
                                    },
                                    onPayInstallment = payAction,
                                    onSetupPlan = setupAction,
                                )
                                if (card.cardType == CreditCardType.INSTALLMENT && nextPlan != null) {
                                    val planScheduled = scheduledByPlan[nextPlan.id].orEmpty()
                                    val pendingPayments = planScheduled.filter { it.status == "PENDING" }
                                    val needsUpdate = totalUtilizedForDisplay > nextPlan.totalAmount
                                    if (needsUpdate) {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(
                                                        "Nuovi acquisti rilevati",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                                    )
                                                    Text(
                                                        "Aggiorna il piano per includere l'importo totale.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                                    )
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        editPlanCard = card
                                                        editPlan = nextPlan
                                                    },
                                                ) { Text("Aggiorna") }
                                            }
                                        }
                                    }
                                    if (pendingPayments.isNotEmpty()) {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        ) {
                                            Column(Modifier.padding(12.dp)) {
                                                Text(
                                                    "Rate future",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                val displayLocale = locale
                                                pendingPayments.forEach { payment ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                    ) {
                                                        val dueLocalDate = try {
                                                            LocalDate.parse(payment.dueDate, DateTimeFormatter.ISO_LOCAL_DATE)
                                                        } catch (_: Exception) {
                                                            null
                                                        }
                                                        Text(
                                                            dueLocalDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", displayLocale)) ?: payment.dueDate,
                                                            style = MaterialTheme.typography.bodySmall,
                                                        )
                                                        Text(
                                                            "$currencySymbol ${String.format(displayLocale, "%.2f", payment.amount)}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.SemiBold,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
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

                        // Card AMEX Ibride
                        val amexPaymentMethods = allPaymentMethods.filter { it.provider == PaymentProvider.CREDIT_CARD_AMEX }
                        if (showAmex && amexPaymentMethods.isNotEmpty()) {
                            val amexByCard = remember(amexStatements) {
                                amexStatements.groupBy { it.paymentMethodId }
                            }
                            val pagoFlexByStatement = remember(amexPagoFlexPlans) {
                                amexPagoFlexPlans.groupBy { it.statementId }
                            }
                            val revolvingByStatement = remember(amexRevolvingStates) {
                                amexRevolvingStates.associateBy { it.statementId }
                            }
                            amexPaymentMethods.forEach { method ->
                                val cardStatements = amexByCard[method.id].orEmpty().sortedByDescending { it.statementMonth }
                                if (cardStatements.isNotEmpty()) {
                                    cardStatements.forEach { statement ->
                                        val pagoFlexPlans = pagoFlexByStatement[statement.id].orEmpty()
                                        val revolvingState = revolvingByStatement[statement.id]
                                        AmexStatementCard(
                                            cardName = method.name,
                                            statement = statement,
                                            pagoFlexPlans = pagoFlexPlans,
                                            revolvingState = revolvingState,
                                            currencySymbol = currencySymbol,
                                            isAmountHidden = isAmountHidden,
                                            locale = locale,
                                            onSetPaymentMode = { mode, amount -> onSetAmexPaymentMode(statement.id, mode, amount) },
                                            onPay = { amount -> onPayAmexStatement(statement, amount) },
                                            onClose = { onCreateAmexStatement(method.id, statement.statementMonth, statement.closingDate, statement.paymentDueDate) },
                                        )
                                    }
                                } else {
                                    AmexStatementCardEmpty(
                                        cardName = method.name,
                                        currencySymbol = currencySymbol,
                                        onCreate = { onCreateAmexStatement(method.id, "", "", "") },
                                    )
                                }
                            }
                        }

                        // AMEX Projections per month
                        if (showAmex && amexProjections.isNotEmpty()) {
                            amexProjections.forEach { proj ->
                                if (proj.pagoflexQuotaTotal > 0.0 || proj.hasDuePayment) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "Amex ${proj.cardName} — Proiezioni",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                            if (proj.pagoflexQuotaTotal > 0.0) {
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    "Rate PagoFlex: $currencySymbol ${String.format(locale, "%.2f", proj.pagoflexQuotaTotal)} (${proj.pagoflexPlanCount} piani)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                )
                                            }
                                            if (proj.hasDuePayment) {
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    "Pagamento in scadenza: $currencySymbol ${String.format(locale, "%.2f", proj.duePaymentAmount)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            ) {
                                Text(
                                    "Pagamento automatico Amex",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.weight(1f))
                                androidx.compose.material3.Switch(
                                    checked = isAmexAutoPayEnabled,
                                    onCheckedChange = onToggleAmexAutoPay,
                                )
                            }
                            if (amexCurrentAccountOutflow > 0.0) {
                                AmexInstallmentCard(
                                    outflowAmount = amexCurrentAccountOutflow,
                                    scheduledPayments = amexScheduledPayments,
                                    plans = amexPagoFlexPlans,
                                    currencySymbol = currencySymbol,
                                    locale = locale,
                                    isAmountHidden = isAmountHidden,
                                    onEditPayment = { planId ->
                                        amexEditPlan = amexPagoFlexPlans.find { it.id == planId }
                                    },
                                )
                            }
                        }

                        amexEditPlan?.let { plan ->
                            val statement = remember(plan, amexStatements) {
                                amexStatements.find { it.id == plan.statementId }
                            }
                            val initialStrategy = when (plan.planType) {
                                "FIXED_AMOUNT" -> AmexInstallmentStrategy.FixedAmount(plan.initialInstallmentAmount ?: plan.installmentAmount)
                                else -> AmexInstallmentStrategy.FixedDuration(plan.installmentCount)
                            }
                            AmexInstallmentSetupDialog(
                                totalAmount = plan.totalAmount,
                                initialStrategy = initialStrategy,
                                onConfirm = { strategy ->
                                    statement?.let { onEditAmexInstallment(plan.id, strategy) }
                                    amexEditPlan = null
                                },
                                onDismiss = { amexEditPlan = null },
                            )
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
                        // Installment Payment Dialog
                        installmentPayDialogCard?.let { card ->
                            installmentPayPlan?.let { plan ->
                                CreditCardInstallmentPaymentDialog(
                                    card = card,
                                    plan = plan,
                                    currencySymbol = currencySymbol,
                                    isAmountHidden = isAmountHidden,
                                    locale = locale,
                                    onDismiss = {
                                        installmentPayDialogCard = null
                                        installmentPayPlan = null
                                    },
                                    onConfirm = { paymentDate ->
                                        onPayInstallmentPlan(plan, card, paymentDate)
                                        installmentPayDialogCard = null
                                        installmentPayPlan = null
                                    },
                                )
                            }
                        }
                        // Installment Plan Setup Dialog
                        setupPlanCard?.let { card ->
                            val summary = creditCardSummaries[card.id]
                            InstallmentPlanSetupDialog(
                                card = card,
                                existingPlan = null,
                                currencySymbol = currencySymbol,
                                locale = locale,
                                totalUtilized = summary?.totalUtilized ?: 0.0,
                                onDismiss = { setupPlanCard = null },
                                onConfirm = { totalAmount, installmentCount, installmentAmount, startDate ->
                                    onSetupInstallmentPlan(card.id, totalAmount, installmentCount, installmentAmount, startDate)
                                    setupPlanCard = null
                                },
                            )
                        }
                        editPlanCard?.let { card ->
                            editPlan?.let { plan ->
                                InstallmentPlanSetupDialog(
                                    card = card,
                                    existingPlan = plan,
                                    currencySymbol = currencySymbol,
                                    locale = locale,
                                    totalUtilized = (creditCardSummaries[card.id]?.totalUtilized ?: 0.0),
                                    onDismiss = {
                                        editPlanCard = null
                                        editPlan = null
                                    },
                                    onConfirm = { totalAmount, installmentCount, installmentAmount, startDate ->
                                        onSetupInstallmentPlan(card.id, totalAmount, installmentCount, installmentAmount, startDate)
                                        editPlanCard = null
                                        editPlan = null
                                    },
                                )
                            }
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
    installmentInfo: String? = null,
    locale: Locale = Locale.getDefault(),
    onPayInstallment: (() -> Unit)? = null,
    onSetupPlan: (() -> Unit)? = null,
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
                    if (type == CreditCardType.REVOLVING || type == CreditCardType.INSTALLMENT || type == CreditCardType.AMEX_HYBRID) {
                        Text(
                            when (type) {
                                CreditCardType.INSTALLMENT -> if (installmentInfo != null) installmentInfo else stringResource(R.string.installment_plan)
                                CreditCardType.AMEX_HYBRID -> "Amex Hybrid"
                                else -> stringResource(R.string.installment_plan)
                            },
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

            if (type == CreditCardType.REVOLVING || type == CreditCardType.INSTALLMENT || type == CreditCardType.AMEX_HYBRID) {
                Text(
                    text = stringResource(R.string.revolving_utilized_label),
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
                val remainingLabel = if (type == CreditCardType.REVOLVING || type == CreditCardType.AMEX_HYBRID) {
                    stringResource(R.string.revolving_remaining_label)
                } else {
                    stringResource(R.string.spent_label)
                }
                Text(
                    text = if (isAmountHidden) "$remainingLabel $currencySymbol *****" else "$remainingLabel $currencySymbol ${String.format(locale, "%.2f", spent)}",
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
            if (onPayInstallment != null || onSetupPlan != null) {
                Spacer(modifier = Modifier.height(12.dp))
                if (onPayInstallment != null) {
                    Button(
                        onClick = onPayInstallment,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.pay_installment))
                    }
                }
                if (onSetupPlan != null) {
                    OutlinedButton(
                        onClick = onSetupPlan,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.setup_installment_plan))
                    }
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

@Composable
private fun CreditCardInstallmentPaymentDialog(
    card: ActiveCreditCard,
    plan: CreditCardInstallmentPlanEntity,
    currencySymbol: String,
    isAmountHidden: Boolean,
    locale: Locale,
    onDismiss: () -> Unit,
    onConfirm: (date: LocalDate) -> Unit,
) {
    val installmentAmount = plan.installmentAmount
    val totalPayments = plan.installmentCount
    val paidCount = plan.paidCount
    val remainingPayments = totalPayments - paidCount
    var dateText by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paga Rata ${card.name}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Piano rateale — $paidCount/$totalPayments rate pagate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Importo rata: $currencySymbol ${String.format(locale, "%.2f", installmentAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Rate rimanenti: $remainingPayments",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Il plafond verrà ripristinato di $currencySymbol ${String.format(locale, "%.2f", installmentAmount)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    val date = try {
                        LocalDate.parse(dateText, DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (_: Exception) {
                        return@TextButton
                    }
                    onConfirm(date)
                },
            ) { Text("Paga") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}

@Composable
private fun InstallmentPlanSetupDialog(
    card: ActiveCreditCard,
    existingPlan: CreditCardInstallmentPlanEntity?,
    currencySymbol: String,
    locale: Locale,
    totalUtilized: Double = 0.0,
    onDismiss: () -> Unit,
    onConfirm: (totalAmount: Double, installmentCount: Int, installmentAmount: Double, startDate: String) -> Unit,
) {
    var calcMode by remember { mutableStateOf("count") }
    var countText by remember(existingPlan) { mutableStateOf((existingPlan?.installmentCount ?: 3).toString()) }
    var amountText by remember(existingPlan) {
        mutableStateOf(
            if (existingPlan != null && existingPlan.totalAmount > 0) {
                "%.0f".format(existingPlan.totalAmount)
            } else {
                "%.0f".format(totalUtilized)
            },
        )
    }
    var perInstallmentText by remember(existingPlan) {
        mutableStateOf(
            if (existingPlan?.installmentAmount != null && existingPlan.installmentAmount > 0) {
                "%.0f".format(existingPlan.installmentAmount)
            } else {
                ""
            },
        )
    }
    var startDateText by remember {
        mutableStateOf(
            if (existingPlan != null) {
                try {
                    LocalDate.parse(existingPlan.startDate, DateTimeFormatter.ISO_LOCAL_DATE)
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                } catch (_: Exception) {
                    LocalDate.now().plusMonths(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                }
            } else {
                val paymentDay = card.paymentDay.coerceIn(1, 28)
                LocalDate.now().plusMonths(1).withDayOfMonth(paymentDay).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            },
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val totalAmount = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val count = countText.toIntOrNull() ?: 1
    val perInstallment = if (calcMode == "count") {
        if (count > 0) totalAmount / count else 0.0
    } else {
        perInstallmentText.replace(',', '.').toDoubleOrNull() ?: 0.0
    }
    val calculatedCount = if (calcMode == "amount" && perInstallment > 0) {
        ceil(totalAmount / perInstallment).toInt()
    } else {
        count
    }
    val lastInstallment = if (calcMode == "amount" && perInstallment > 0) {
        totalAmount - (perInstallment * (calculatedCount - 1))
    } else if (calcMode == "count" && count > 0) {
        totalAmount - (perInstallment * (count - 1))
    } else {
        perInstallment
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingPlan != null) "Modifica piano — ${card.name}" else "Nuovo piano — ${card.name}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Totale da rateizzare:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Importo totale") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = calcMode == "count",
                        onClick = { calcMode = "count" },
                        label = { Text("N° rate") },
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = calcMode == "amount",
                        onClick = { calcMode = "amount" },
                        label = { Text("Importo rata") },
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (calcMode == "count") {
                    OutlinedTextField(
                        value = countText,
                        onValueChange = { countText = it.filter { c -> c.isDigit() } },
                        label = { Text("Numero rate") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (count > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Importo rata: $currencySymbol ${String.format(locale, "%.2f", perInstallment)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (lastInstallment != perInstallment) {
                            Text(
                                "Ultima rata: $currencySymbol ${String.format(locale, "%.2f", lastInstallment)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = perInstallmentText,
                        onValueChange = { perInstallmentText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                        label = { Text("Importo per rata") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (perInstallment > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Rate: $calculatedCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (lastInstallment != perInstallment) {
                            Text(
                                "Ultima rata: $currencySymbol ${String.format(locale, "%.2f", lastInstallment)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = startDateText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Data prima rata") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, "Seleziona data")
                        }
                    },
                )
                if (showDatePicker) {
                    val dpState = rememberDatePickerState(
                        initialSelectedDateMillis = try {
                            LocalDate.parse(startDateText, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        } catch (_: Exception) {
                            LocalDate.now().plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        },
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                dpState.selectedDateMillis?.let { millis ->
                                    startDateText = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                }
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Annulla") }
                        },
                    ) {
                        DatePicker(state = dpState)
                    }
                }
                if (existingPlan != null && existingPlan.paidCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${existingPlan.paidCount} rate già pagate — verranno preservate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = totalAmount > 0 && calculatedCount > 0 && perInstallment > 0,
                onClick = {
                    val isoStartDate = try {
                        LocalDate.parse(startDateText, DateTimeFormatter.ofPattern("dd/MM/yyyy")).format(DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (_: Exception) {
                        LocalDate.now().plusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                    onConfirm(
                        totalAmount,
                        calculatedCount,
                        perInstallment,
                        isoStartDate,
                    )
                    onDismiss()
                },
            ) { Text("Salva piano") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}

@Composable
private fun AmexStatementCard(
    cardName: String,
    statement: AmexStatementEntity,
    pagoFlexPlans: List<AmexPagoFlexPlanEntity>,
    revolvingState: AmexRevolvingStateEntity?,
    currencySymbol: String,
    isAmountHidden: Boolean,
    locale: Locale,
    onSetPaymentMode: (AmexPaymentMode, Double) -> Unit,
    onPay: (Double) -> Unit,
    onClose: () -> Unit,
) {
    val summary = remember(statement, pagoFlexPlans, revolvingState) {
        val useCase = com.expense.management.domain.usecase.CalculateAmexStatementUseCase()
        useCase.execute(statement, pagoFlexPlans, revolvingState)
    }
    var showModeMenu by remember { mutableStateOf(false) }
    var showPayDialog by remember { mutableStateOf(false) }
    var customAmount by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CreditCard, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Amex $cardName — ${statement.statementMonth}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Totale spese: ${if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(locale, "%.2f", summary.totalExpenses)}"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (summary.totalPagoflex > 0.0) {
                val pagoFlexCount = pagoFlexPlans.sumOf { it.installmentCount }
                Text(
                    "PagoFlex: $pagoFlexCount rate x $currencySymbol ${String.format(locale, "%.2f", summary.pagoflexQuota)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (revolvingState != null && revolvingState.carriedForwardDebt > 0.0) {
                Text(
                    "Saldo portato: $currencySymbol ${String.format(locale, "%.2f", revolvingState.carriedForwardDebt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (summary.paymentAmount > 0.0) {
                Text(
                    "Da pagare: $currencySymbol ${String.format(locale, "%.2f", summary.paymentAmount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showModeMenu = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        statement.paymentMode.let {
                            @Suppress("UNUSED_EXPRESSION")
                            when (it) {
                                AmexPaymentMode.SALDO.name -> "Saldo"
                                AmexPaymentMode.MINIMUM.name -> "Minimo"
                                AmexPaymentMode.FIXED.name -> "Importo fisso"
                                AmexPaymentMode.PAGOFLEX_ONLY.name -> "Solo PagoFlex"
                                else -> "Saldo"
                            }
                        },
                    )
                }
                OutlinedButton(
                    onClick = { showPayDialog = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Paga")
                }
            }
        }
    }
    if (showModeMenu) {
        var selectedMode by remember { mutableStateOf(AmexPaymentMode.SALDO) }
        AlertDialog(
            onDismissRequest = { showModeMenu = false },
            title = { Text("Modalità pagamento") },
            text = {
                Column {
                    AmexPaymentMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = {
                                Text(
                                    when (mode) {
                                        AmexPaymentMode.SALDO -> "Saldo completo"
                                        AmexPaymentMode.MINIMUM -> "Pagamento minimo"
                                        AmexPaymentMode.FIXED -> "Importo fisso"
                                        AmexPaymentMode.PAGOFLEX_ONLY -> "Solo PagoFlex"
                                    },
                                )
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        )
                    }
                    if (selectedMode == AmexPaymentMode.FIXED) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customAmount,
                            onValueChange = { customAmount = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Importo") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = if (selectedMode == AmexPaymentMode.FIXED) customAmount.toDoubleOrNull() ?: 0.0 else 0.0
                    onSetPaymentMode(selectedMode, amount)
                    showModeMenu = false
                }) { Text("Conferma") }
            },
            dismissButton = {
                TextButton(onClick = { showModeMenu = false }) { Text("Annulla") }
            },
        )
    }
    if (showPayDialog) {
        AlertDialog(
            onDismissRequest = { showPayDialog = false },
            title = { Text("Paga estratto conto") },
            text = {
                Column {
                    Text("Importo da pagare: $currencySymbol ${String.format(locale, "%.2f", summary.paymentAmount)}")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customAmount,
                        onValueChange = { customAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Importo (lascia vuoto per il totale)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = customAmount.toDoubleOrNull() ?: summary.paymentAmount
                    onPay(amount)
                    showPayDialog = false
                }) { Text("Paga") }
            },
            dismissButton = {
                TextButton(onClick = { showPayDialog = false }) { Text("Annulla") }
            },
        )
    }
}

@Composable
private fun AmexStatementCardEmpty(
    cardName: String,
    currencySymbol: String,
    onCreate: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Amex $cardName",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Nessun estratto conto aperto.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Crea estratto conto")
            }
        }
    }
}

@Preview(showBackground = true, name = "Dashboard Light")
@Composable
private fun DashboardPreview() {
    AppTheme(appStyle = AppStyle.MATERIAL_YOU, darkTheme = false) {
        DashboardScreen(transactions = emptyList(), categories = emptyList(), currencySymbol = "€", dateFormat = "dd/MM/yyyy", earliestMonth = java.time.YearMonth.now(), currentDashboardMonth = java.time.YearMonth.now(), onMonthChange = {}, onDelete = { _, _ -> }, onEdit = { _, _ -> }, isAmountHidden = false)
    }
}

@Preview(showBackground = true, name = "Dashboard Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardPreviewDark() {
    AppTheme(appStyle = AppStyle.MATERIAL_YOU, darkTheme = true) {
        DashboardScreen(transactions = emptyList(), categories = emptyList(), currencySymbol = "€", dateFormat = "dd/MM/yyyy", earliestMonth = java.time.YearMonth.now(), currentDashboardMonth = java.time.YearMonth.now(), onMonthChange = {}, onDelete = { _, _ -> }, onEdit = { _, _ -> }, isAmountHidden = false)
    }
}
