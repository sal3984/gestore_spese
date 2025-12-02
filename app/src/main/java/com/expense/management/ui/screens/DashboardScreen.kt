package com.expense.management.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.data.CategoryEntity
import com.expense.management.data.TransactionEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// Enum per il tipo di cancellazione
enum class DeleteType {
    SINGLE,
    THIS_AND_SUBSEQUENT
}

// Data class per gestire la transazione da eliminare
data class TransactionToDelete(
    val transaction: TransactionEntity,
    val isInstallment: Boolean
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    onDelete: (String, DeleteType) -> Unit, // Modificata la firma di onDelete
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

    // LIMITI NAVIGAZIONE
    val minMonth = if (earliestMonth.isBefore(today.minusMonths(3))) earliestMonth else today.minusMonths(3)
    val maxMonth = today.plusMonths(12)

    // State for entrance animation
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

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
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.delete_this_and_subsequent))
                        }
                        TextButton(
                            onClick = {
                                onDelete(transactionToDelete.transaction.id, DeleteType.SINGLE)
                                showDeleteDialog = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
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
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.delete_uppercase))
                    }
                }
            },
            dismissButton = { /* Vuoto, il pulsante Annulla è ora nel confirmButton's Column */ }
        )
    }

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
                        .padding(bottom = 48.dp)
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
                                tint = Color.White
                            )
                        }

                        Text(
                            currentDashboardMonth.format(monthFormatter).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        IconButton(
                            onClick = { onMonthChange(currentDashboardMonth.plusMonths(1)) },
                            enabled = currentDashboardMonth < maxMonth
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.next_month),
                                tint = Color.White
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
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(Locale.getDefault(), "%.2f", netBalance)}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- CARDS ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-40).dp)
                        .padding(horizontal = 16.dp)
                ) {
                    // Card Entrate/Uscite
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(24.dp),
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

                            // Uscite
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = fadeIn(animationSpec = tween(durationMillis = 600)) + slideInVertically(initialOffsetY = { it / 2 })
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nessuna transazione\nin questo mese",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aggiungi una nuova spesa o entrata\nper iniziare a tracciare i tuoi movimenti.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            groupedTransactions.forEach { (dateString, transactions) ->
                stickyHeader {
                    DateHeader(dateString)
                }
                items(transactions, key = { it.id }) { t ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                showDeleteDialog = TransactionToDelete(
                                    transaction = t,
                                    isInstallment = t.isCreditCard && t.installmentNumber != null && t.totalInstallments != null
                                )
                                false // Mantieni l'elemento in posizione fino alla conferma
                            } else {
                                false
                            }
                        }
                    )

                    // Wrapper per animazione di entrata semplice
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(initialOffsetY = { 50 }),
                        modifier = Modifier.animateItem()
                    ) {
                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = Modifier.padding(vertical = 1.dp),
                            enableDismissFromStartToEnd = false, // Disabilita swipe da inizio a fine
                            enableDismissFromEndToStart = true, // Abilita swipe da fine a inizio
                            backgroundContent = {
                                val color = when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.EndToStart -> Color.Red
                                    else -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = Color.White
                                    )
                                }
                            },
                            content = {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    TransactionItem(
                                        transaction = t,
                                        categories = categories,
                                        currencySymbol = currencySymbol,
                                        dateFormat = dateFormat,
                                        isAmountHidden = isAmountHidden,
                                        onDelete = { /* La cancellazione è gestita dallo SwipeToDismissBox */ },
                                        onEdit = onEdit
                                    )
                                }
                            }
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
            .padding(vertical = 8.dp, horizontal = 16.dp)
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
