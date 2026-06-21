package com.expense.management.ui.screens.amex

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
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
    currencySymbol: String,
    locale: Locale,
    isAmountHidden: Boolean,
    onEditPayment: (planId: String) -> Unit,
) {
    val currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    val pendingPayments = remember(scheduledPayments, currentMonth) {
        scheduledPayments.filter { it.status == "PENDING" && it.dueDate.startsWith(currentMonth) }
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
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Totale mese: ${if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(locale, "%.2f", outflowAmount)}"}",
                style = MaterialTheme.typography.titleSmall,
            )
            pendingPayments.forEach { payment ->
                val plan = plans.find { it.id == payment.planId }
                val parsedDueDate = runCatching { LocalDate.parse(payment.dueDate) }.getOrNull()
                val isCurrentMonthEditable = payment.dueDate.startsWith(currentMonth) && parsedDueDate != null && !parsedDueDate.isBefore(LocalDate.now())
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        .then(if (isCurrentMonthEditable) Modifier.clickable { onEditPayment(payment.planId) } else Modifier.alpha(0.7f)),
                ) {
                    Text(
                        "Rata ${payment.sequenceNumber}/${plan?.installmentCount ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (isAmountHidden) "$currencySymbol *****" else "$currencySymbol ${String.format(locale, "%.2f", payment.amount)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
