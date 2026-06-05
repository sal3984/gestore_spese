package com.expense.management.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.ui.screens.settingsSectionHeader
import com.expense.management.ui.theme.AppStyle
import com.expense.management.ui.theme.AppTheme

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

        if (allPaymentMethods.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    allPaymentMethods.forEachIndexed { index, method ->
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
                        if (index < allPaymentMethods.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
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
}

@Preview(showBackground = true, name = "Payment Settings Light")
@Composable
private fun PaymentSettingsPreviewLight() {
    AppTheme(appStyle = AppStyle.MATERIAL_YOU, darkTheme = false) {
        PaymentSettingsScreen(
            allPaymentMethods = emptyList(),
            defaultPaymentMethodId = "__cash__",
            onDefaultPaymentMethodChange = {},
            onNavigateToPaymentMethods = {},
        )
    }
}

@Preview(showBackground = true, name = "Payment Settings Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PaymentSettingsPreviewDark() {
    AppTheme(appStyle = AppStyle.MATERIAL_YOU, darkTheme = true) {
        PaymentSettingsScreen(
            allPaymentMethods = emptyList(),
            defaultPaymentMethodId = "__cash__",
            onDefaultPaymentMethodChange = {},
            onNavigateToPaymentMethods = {},
        )
    }
}
