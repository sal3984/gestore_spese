package com.expense.management.ui.screens.amex

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.data.AmexPagoFlexPlanEntity
import com.expense.management.data.AmexPagoFlexScheduledPaymentEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AmexInstallmentCard(
    outflowAmount: Double,
    scheduledPayments: List<AmexPagoFlexScheduledPaymentEntity>,
    plans: List<AmexPagoFlexPlanEntity>,
    selectedMonth: YearMonth,
    currencySymbol: String,
    locale: Locale,
    isAmountHidden: Boolean,
    onEditPayment: (planId: String) -> Unit,
) {
    val monthPrefix = selectedMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    val monthPayments = remember(scheduledPayments, monthPrefix) {
        scheduledPayments.filter { it.dueDate.startsWith(monthPrefix) }
            .sortedBy { it.sequenceNumber }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AttachMoney, contentDescription = stringResource(R.string.amex_installment_icon_cd), tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Uscite conto corrente Amex",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Totale mese: ${if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(locale, "%.2f", outflowAmount)}"}",
                style = MaterialTheme.typography.titleSmall,
            )
            monthPayments.forEach { payment ->
                val plan = plans.find { it.id == payment.planId }
                val parsedDueDate = runCatching { LocalDate.parse(payment.dueDate) }.getOrNull()
                val isPaid = payment.status == "PAID"
                val isCurrentMonthEditable = !isPaid && payment.dueDate.startsWith(monthPrefix) && parsedDueDate != null && !parsedDueDate.isBefore(LocalDate.now())
                val installmentLabel = "Rata ${payment.sequenceNumber}/${plan?.installmentCount ?: "-"}"
                val amountLabel = if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(locale, "%.2f", payment.amount)}"
                val rowContentDescription = "$installmentLabel, $amountLabel" + if (isPaid) ", pagata" else if (isCurrentMonthEditable) ", tocca per modificare" else ", in attesa"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 6.dp)
                        .then(
                            if (isCurrentMonthEditable) {
                                Modifier.toggleable(
                                    role = Role.Button,
                                    value = false,
                                    onValueChange = { onEditPayment(payment.planId) },
                                ).semantics { contentDescription = rowContentDescription }
                            } else {
                                Modifier.alpha(if (isPaid) 0.5f else 0.7f).semantics { contentDescription = rowContentDescription }
                            },
                        ),
                ) {
                    Text(
                        installmentLabel,
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = if (isPaid) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        amountLabel,
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = if (isPaid) TextDecoration.LineThrough else TextDecoration.None,
                    )
                }
            }
        }
    }
}
