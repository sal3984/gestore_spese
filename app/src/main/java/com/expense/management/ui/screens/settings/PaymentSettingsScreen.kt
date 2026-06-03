package com.expense.management.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.ui.screens.settingsSectionHeader

@Composable
fun PaymentSettingsScreen(
    allPaymentMethods: List<PaymentMethodEntity>,
    defaultPaymentMethodId: String,
    onDefaultPaymentMethodChange: (String) -> Unit,
    onNavigateToPaymentMethods: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        settingsSectionHeader(stringResource(R.string.default_payment_method))

        allPaymentMethods.forEach { method ->
            ListItem(
                headlineContent = {
                    Text(method.name, style = MaterialTheme.typography.bodyLarge)
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Payment,
                        contentDescription = method.name,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                },
                trailingContent = {
                    RadioButton(
                        selected = method.id == defaultPaymentMethodId,
                        onClick = null,
                    )
                },
                modifier = Modifier
                    .clickable { onDefaultPaymentMethodChange(method.id) }
                    .heightIn(min = 48.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        ListItem(
            headlineContent = {
                Text(
                    stringResource(R.string.manage_payment_methods),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            leadingContent = {
                Icon(
                    Icons.Default.CreditCard,
                    contentDescription = stringResource(R.string.payment_methods),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            },
            trailingContent = {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.more),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            modifier = Modifier.clickable { onNavigateToPaymentMethods() },
        )
    }
}
