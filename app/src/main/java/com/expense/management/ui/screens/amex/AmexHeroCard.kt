package com.expense.management.ui.screens.amex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.domain.model.AmexStatementWithDetails
import java.util.Locale

@Composable
fun AmexHeroCard(
    cardName: String,
    statement: AmexStatementWithDetails,
    currencySymbol: String,
    locale: Locale,
    isAmountHidden: Boolean,
    onPay: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = statement.summary
    var showPayDialog by remember { mutableStateOf(false) }
    var customAmount by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CreditCard, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.amex_card_statement, cardName, statement.statement.statementMonth),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(
                    R.string.amex_total_expenses,
                    if (isAmountHidden) stringResource(R.string.amex_amount_hidden, currencySymbol) else "$currencySymbol ${String.format(locale, "%.2f", summary.totalExpenses)}",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (summary.totalPagoflex > 0.0) {
                val pagoFlexCount = statement.pagoFlexPlans.sumOf { it.installmentCount }
                Text(
                    stringResource(R.string.amex_pagoflex_summary, pagoFlexCount, "$currencySymbol ${String.format(locale, "%.2f", summary.pagoflexQuota)}"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (summary.carriedForward > 0.0) {
                Text(
                    stringResource(R.string.amex_carried_forward, "$currencySymbol ${String.format(locale, "%.2f", summary.carriedForward)}"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (summary.paymentAmount > 0.0) {
                Text(
                    stringResource(R.string.amex_to_pay, "$currencySymbol ${String.format(locale, "%.2f", summary.paymentAmount)}"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showPayDialog = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.amex_pay))
                }
            }
        }
    }

    if (showPayDialog) {
        AlertDialog(
            onDismissRequest = { showPayDialog = false },
            title = { Text(stringResource(R.string.amex_pay_statement)) },
            text = {
                Column {
                    Text(stringResource(R.string.amex_amount_to_pay, "$currencySymbol ${String.format(locale, "%.2f", summary.paymentAmount)}"))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customAmount,
                        onValueChange = { customAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.amex_amount_hint_total)) },
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
                }) { Text(stringResource(R.string.amex_pay)) }
            },
            dismissButton = {
                TextButton(onClick = { showPayDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
