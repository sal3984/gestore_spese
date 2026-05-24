package com.expense.management.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.data.CardType
import com.expense.management.data.CreditCardEntity
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.domain.model.PaymentProvider
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodSettingsScreen(
    currentCurrency: String,
    allPaymentMethods: List<PaymentMethodEntity>,
    legacyCreditCards: List<CreditCardEntity>,
    onNavigateBack: () -> Unit,
    onAdd: (PaymentMethodEntity) -> Unit,
    onDelete: (String) -> Unit,
    onAddLegacyCard: (CreditCardEntity) -> Unit,
    onUpdateLegacyCard: (CreditCardEntity) -> Unit,
    onDeleteLegacyCard: (CreditCardEntity) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.payment_methods), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.add_payment_method))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Legacy credit cards section
            if (legacyCreditCards.isNotEmpty()) {
                settingsSectionHeader(stringResource(R.string.credit_cards))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                ) {
                    legacyCreditCards.forEach { card ->
                        paymentMethodListItem(
                            icon = Icons.Default.CreditCard,
                            title = card.name,
                            subtitle = "${if (card.type == CardType.SALDO) stringResource(R.string.single_balance) else stringResource(R.string.installment_plan)} • ${stringResource(R.string.max_limit, currentCurrency)} ${String.format(Locale.US, "%.0f", card.limit)}",
                            onEdit = { /* TODO: edit legacy card */ },
                            onDelete = { onDeleteLegacyCard(card) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            }

            // New payment methods section
            if (allPaymentMethods.isEmpty() && legacyCreditCards.isEmpty()) {
                Text(
                    stringResource(R.string.no_payment_methods),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                allPaymentMethods.groupBy { it.provider }.forEach { (provider, methods) ->
                    settingsSectionHeader(providerLabel(provider))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    ) {
                        methods.forEach { method ->
                            paymentMethodListItem(
                                icon = providerIcon(provider),
                                title = method.name,
                                subtitle = method.issuer ?: providerLabel(provider),
                                onEdit = { /* TODO: edit payment method details */ },
                                onDelete = { onDelete(method.id) },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Add Payment Method Dialog
    if (showAddDialog) {
        AddPaymentMethodDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { provider, name ->
                val newMethod = PaymentMethodEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    provider = provider.name,
                    isActive = true,
                    issuer = null,
                    currency = null,
                )
                onAdd(newMethod)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun paymentMethodListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        },
        trailingContent = {
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun AddPaymentMethodDialog(
    onDismiss: () -> Unit,
    onConfirm: (PaymentProvider, String) -> Unit,
) {
    var selectedProvider by remember { mutableStateOf(PaymentProvider.CREDIT_CARD_SALDO) }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_payment_method)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.payment_method_name)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                )

                Text(stringResource(R.string.provider), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                Column {
                    PaymentProvider.entries.forEach { provider ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = selectedProvider == provider,
                                onClick = { selectedProvider = provider },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                providerIcon(provider),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(providerLabel(provider))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(selectedProvider, name)
                    }
                },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun providerLabel(provider: PaymentProvider): String {
    return when (provider) {
        PaymentProvider.CREDIT_CARD_SALDO -> "Carta di Credito (Saldo)"
        PaymentProvider.CREDIT_CARD_REVOLVING -> "Carta di Credito (Revolving)"
        PaymentProvider.REVOLUT -> "Revolut"
        PaymentProvider.SATISPAY -> "Satispay"
        PaymentProvider.PAYPAL -> "PayPal"
        PaymentProvider.KLARNA -> "Klarna"
    }
}

private fun providerIcon(provider: PaymentProvider): ImageVector {
    return when (provider) {
        PaymentProvider.CREDIT_CARD_SALDO -> Icons.Default.CreditCard
        PaymentProvider.CREDIT_CARD_REVOLVING -> Icons.Default.CreditCard
        PaymentProvider.REVOLUT -> Icons.Default.Savings
        PaymentProvider.SATISPAY -> Icons.Default.Star
        PaymentProvider.PAYPAL -> Icons.Default.Payment
        PaymentProvider.KLARNA -> Icons.Default.ShoppingCart
    }
}

private fun providerLabel(provider: String): String {
    return try {
        providerLabel(PaymentProvider.valueOf(provider))
    } catch (_: Exception) {
        provider
    }
}

private fun providerIcon(provider: String): ImageVector {
    return try {
        providerIcon(PaymentProvider.valueOf(provider))
    } catch (_: Exception) {
        Icons.Default.Payment
    }
}
