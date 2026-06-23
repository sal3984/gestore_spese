package com.expense.management.ui.screens.amex

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.domain.model.AmexCardHubData
import com.expense.management.domain.model.AmexHubData
import com.expense.management.domain.model.AmexInstallmentStrategy
import java.time.YearMonth
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmexHubScreen(
    hubData: AmexHubData,
    selectedMonth: YearMonth,
    currencySymbol: String,
    locale: Locale,
    isAmountHidden: Boolean,
    onMonthChange: (YearMonth) -> Unit,
    onPayStatement: (statementId: String, amount: Double) -> Unit,
    onEditInstallment: (planId: String, strategy: AmexInstallmentStrategy) -> Unit,
    onToggleAutoPay: (Boolean) -> Unit,
    onPayInstallment: (paymentId: String) -> Unit,
    onCreateStatement: (paymentMethodId: String, statementMonth: String, closingDate: String, paymentDueDate: String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val pagerState = rememberPagerState(
        pageCount = { maxOf(1, hubData.cards.size) },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.amex_hub_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        if (hubData.cards.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(R.string.amex_no_cards),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) { page ->
                val cardData = hubData.cards.getOrNull(page)
                if (cardData != null) {
                    AmexHubCardPage(
                        cardData = cardData,
                        currencySymbol = currencySymbol,
                        locale = locale,
                        isAmountHidden = isAmountHidden,
                        selectedMonth = selectedMonth,
                        isAutoPayEnabled = hubData.autoPayEnabled,
                        onPayInstallment = onPayInstallment,
                        onPayStatement = onPayStatement,
                        onEditInstallment = onEditInstallment,
                        onToggleAutoPay = onToggleAutoPay,
                        onCreateStatement = onCreateStatement,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { /* handled by pager */ },
                    enabled = pagerState.currentPage > 0,
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.amex_prev))
                }

                Spacer(Modifier.width(8.dp))
                Text(
                    hubData.cards.getOrNull(pagerState.currentPage)?.cardName ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = { /* handled by pager */ },
                    enabled = pagerState.currentPage < hubData.cards.size - 1,
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.amex_next))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(hubData.cards.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                    val pageDesc = stringResource(R.string.amex_page_indicator, iteration + 1, pagerState.pageCount)
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(8.dp)
                            .semantics {
                                contentDescription = pageDesc
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun AmexHubCardPage(
    cardData: AmexCardHubData,
    currencySymbol: String,
    locale: Locale,
    isAmountHidden: Boolean,
    selectedMonth: YearMonth,
    isAutoPayEnabled: Boolean,
    onPayInstallment: (paymentId: String) -> Unit,
    onPayStatement: (statementId: String, amount: Double) -> Unit,
    onEditInstallment: (planId: String, strategy: AmexInstallmentStrategy) -> Unit,
    onToggleAutoPay: (Boolean) -> Unit,
    onCreateStatement: (paymentMethodId: String, statementMonth: String, closingDate: String, paymentDueDate: String) -> Unit,
) {
    val openStatements = cardData.statements.filter { statement ->
        !statement.statement.isClosed
    }

    var editingPlan by remember { mutableStateOf<AmexPagoFlexPlanEntity?>(null) }

    val allPlans = cardData.statements.flatMap { it.pagoFlexPlans }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        if (openStatements.isEmpty()) {
            item {
                AmexCardEmpty(
                    cardName = cardData.cardName,
                    onCreate = {
                        onCreateStatement(cardData.paymentMethodId, "", "", "")
                    },
                )
            }
        } else {
            openStatements.take(1).forEach { statement ->
                item {
                    AmexHeroCard(
                        cardName = cardData.cardName,
                        statement = statement,
                        currencySymbol = currencySymbol,
                        locale = locale,
                        isAmountHidden = isAmountHidden,
                        onPay = { amount ->
                            onPayStatement(statement.statement.id, amount)
                        },
                    )
                }
            }
        }

        cardData.projection?.let { projection ->
            if (projection.pagoflexQuotaTotal > 0.0 || projection.hasDuePayment) {
                item {
                    AmexProjectionsCard(
                        projection = projection,
                        currencySymbol = currencySymbol,
                        locale = locale,
                    )
                }
            }
        }

        item {
            AutoPayToggle(
                isEnabled = isAutoPayEnabled,
                onToggle = onToggleAutoPay,
            )
        }

        val allPlans = cardData.statements.flatMap { it.pagoFlexPlans }
        if (cardData.scheduledPayments.isNotEmpty()) {
            item {
                AmexInstallmentCard(
                    scheduledPayments = cardData.scheduledPayments,
                    plans = allPlans,
                    currencySymbol = currencySymbol,
                    locale = locale,
                    isAmountHidden = isAmountHidden,
                    onPayInstallment = onPayInstallment,
                    onEditPlan = { planId ->
                        editingPlan = allPlans.find { it.id == planId }
                    },
                )
            }
        }
    }

    editingPlan?.let { plan ->
        val paidAmount = cardData.scheduledPayments
            .filter { it.planId == plan.id && it.status == "PAID" }
            .sumOf { it.amount }
        val residual = (plan.totalAmount - paidAmount).coerceAtLeast(0.0)
        val initialStrategy = when (plan.planType) {
            "FIXED_AMOUNT" -> AmexInstallmentStrategy.FixedAmount(plan.initialInstallmentAmount ?: plan.installmentAmount)
            else -> AmexInstallmentStrategy.FixedDuration(plan.installmentCount)
        }
        AmexInstallmentSetupDialog(
            totalAmount = residual,
            currencySymbol = currencySymbol,
            initialStrategy = initialStrategy,
            onConfirm = { strategy ->
                onEditInstallment(plan.id, strategy)
                editingPlan = null
            },
            onDismiss = { editingPlan = null },
        )
    }
}

@Composable
private fun AmexCardEmpty(
    cardName: String,
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
                stringResource(R.string.amex_card_name, cardName),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.amex_no_statement_open),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.amex_create_statement))
            }
        }
    }
}

@Composable
private fun AutoPayToggle(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)
            .heightIn(min = 48.dp)
            .toggleable(
                value = isEnabled,
                role = Role.Switch,
                onValueChange = onToggle,
            ),
    ) {
        Text(
            stringResource(R.string.amex_autopay),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        androidx.compose.material3.Switch(
            checked = isEnabled,
            onCheckedChange = null,
        )
    }
}
