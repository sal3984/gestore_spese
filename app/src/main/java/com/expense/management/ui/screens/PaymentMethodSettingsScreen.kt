package com.expense.management.ui.screens

import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.data.CardType
import com.expense.management.data.CreditCardEntity
import com.expense.management.data.PaymentMethodEntity
import com.expense.management.domain.model.CreditCardType
import com.expense.management.domain.model.PaymentMethodDetails
import com.expense.management.domain.model.PaymentProvider
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodSettingsScreen(
    currentCurrency: String,
    allPaymentMethods: List<PaymentMethodEntity>,
    legacyCreditCards: List<CreditCardEntity>,
    onNavigateBack: () -> Unit,
    onAdd: (PaymentMethodEntity, closingDay: Int, paymentDay: Int, debitIssuer: String?, debitCardNumber: String?, debitNotes: String?, creditLimit: Double) -> Unit,
    onDelete: (String) -> Unit,
    onEditPaymentMethod: (PaymentMethodEntity, PaymentMethodDetails) -> Unit,
    onLoadDetails: suspend (String) -> PaymentMethodDetails?,
    onAddLegacyCard: (CreditCardEntity) -> Unit,
    onUpdateLegacyCard: (CreditCardEntity) -> Unit,
    onDeleteLegacyCard: (CreditCardEntity) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMethod by remember { mutableStateOf<PaymentMethodEntity?>(null) }
    var editingLegacyCard by remember { mutableStateOf<CreditCardEntity?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_payment_method))
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
                            onEdit = { editingLegacyCard = card },
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
                                onEdit = { editingMethod = method },
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
            onConfirm = { provider, name, closingDay, _, paymentDay, debitIssuer, debitCardNumber, debitNotes, creditLimit ->
                val newMethod = PaymentMethodEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    provider = provider,
                    isActive = true,
                    issuer = null,
                    currency = null,
                )
                onAdd(newMethod, closingDay, paymentDay, debitIssuer, debitCardNumber, debitNotes, creditLimit)
                showAddDialog = false
            },
        )
    }

    // Edit Payment Method Dialog
    editingMethod?.let { method ->
        var currentDetails by remember { mutableStateOf<PaymentMethodDetails?>(null) }
        LaunchedEffect(method.id) {
            currentDetails = onLoadDetails(method.id)
        }
        val provider = method.provider
        EditPaymentMethodDialog(
            provider = provider,
            currentDetails = currentDetails,
            onDismiss = { editingMethod = null },
            onSave = { name, details ->
                val updatedMethod = method.copy(name = name)
                onEditPaymentMethod(updatedMethod, details)
                editingMethod = null
            },
        )
    }

    // Edit Legacy Credit Card Dialog
    editingLegacyCard?.let { card ->
        var editName by remember(card) { mutableStateOf(card.name) }
        var editLimitText by remember(card) { mutableStateOf(card.limit.toString()) }
        var editClosingDayText by remember(card) { mutableStateOf(card.closingDay.toString()) }
        var editPaymentDayText by remember(card) { mutableStateOf(card.paymentDay.toString()) }
        var isLastDayOfMonth by remember(card) { mutableStateOf(card.closingDay >= 31) }

        AlertDialog(
            onDismissRequest = { editingLegacyCard = null },
            title = { Text(stringResource(R.string.edit_card)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(stringResource(R.string.card_name)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editLimitText,
                        onValueChange = { editLimitText = it },
                        label = { Text(stringResource(R.string.card_limit)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isLastDayOfMonth,
                            onCheckedChange = {
                                isLastDayOfMonth = it
                                if (it) editClosingDayText = ""
                            },
                        )
                        Text(stringResource(R.string.end_of_month), style = MaterialTheme.typography.bodySmall)
                    }
                    if (!isLastDayOfMonth) {
                        OutlinedTextField(
                            value = editClosingDayText,
                            onValueChange = {
                                editClosingDayText = it
                                if (it.isNotEmpty()) isLastDayOfMonth = false
                            },
                            label = { Text(stringResource(R.string.closing_day)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = editPaymentDayText,
                        onValueChange = { editPaymentDayText = it },
                        label = { Text(stringResource(R.string.payment_day)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val limit = editLimitText.toDoubleOrNull() ?: return@TextButton
                    val closingDay = if (isLastDayOfMonth) 31 else (editClosingDayText.toIntOrNull() ?: return@TextButton)
                    val paymentDay = editPaymentDayText.toIntOrNull() ?: return@TextButton
                    onUpdateLegacyCard(card.copy(name = editName, limit = limit, closingDay = closingDay, paymentDay = paymentDay))
                    editingLegacyCard = null
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { editingLegacyCard = null }) { Text(stringResource(R.string.cancel)) } },
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
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        },
        trailingContent = {
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
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
    onConfirm: (PaymentProvider, String, Int, Boolean, Int, String?, String?, String?, Double) -> Unit,
) {
    var selectedProvider by remember { mutableStateOf(PaymentProvider.CREDIT_CARD_SALDO) }
    var name by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("1000") }
    var closingDayText by remember { mutableStateOf("") }
    var paymentDayText by remember { mutableStateOf("") }
    var isLastDayOfMonth by remember { mutableStateOf(true) }
    var debitIssuer by remember { mutableStateOf("") }
    var debitCardNumber by remember { mutableStateOf("") }
    var debitNotes by remember { mutableStateOf("") }
    val isCreditCard = selectedProvider == PaymentProvider.CREDIT_CARD_SALDO ||
        selectedProvider == PaymentProvider.CREDIT_CARD_REVOLVING
    val isDebitCard = selectedProvider == PaymentProvider.DEBIT_CARD

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_payment_method)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                                contentDescription = providerLabel(provider),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(providerLabel(provider))
                        }
                    }
                }

                if (isCreditCard) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it },
                        label = { Text(stringResource(R.string.card_limit)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isLastDayOfMonth,
                            onCheckedChange = { isLastDayOfMonth = it },
                        )
                        Text(stringResource(R.string.end_of_month), style = MaterialTheme.typography.bodySmall)
                    }
                    if (!isLastDayOfMonth) {
                        OutlinedTextField(
                            value = closingDayText,
                            onValueChange = {
                                closingDayText = it
                                if (it.isNotEmpty()) isLastDayOfMonth = false
                            },
                            label = { Text(stringResource(R.string.closing_day)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = paymentDayText,
                        onValueChange = { paymentDayText = it },
                        label = { Text(stringResource(R.string.payment_day)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (isDebitCard) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    OutlinedTextField(
                        value = debitIssuer,
                        onValueChange = { debitIssuer = it },
                        label = { Text(stringResource(R.string.issuer_bank)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                    OutlinedTextField(
                        value = debitCardNumber,
                        onValueChange = { debitCardNumber = it },
                        label = { Text(stringResource(R.string.card_number)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                    OutlinedTextField(
                        value = debitNotes,
                        onValueChange = { debitNotes = it },
                        label = { Text(stringResource(R.string.notes)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        val closingDay = if (isLastDayOfMonth) 31 else (closingDayText.toIntOrNull() ?: 31)
                        val paymentDay = paymentDayText.toIntOrNull() ?: 15
                        onConfirm(
                            selectedProvider,
                            name,
                            closingDay,
                            isLastDayOfMonth,
                            paymentDay,
                            debitIssuer.ifBlank { null },
                            debitCardNumber.ifBlank { null },
                            debitNotes.ifBlank { null },
                            limitText.toDoubleOrNull() ?: 0.0,
                        )
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

@Composable
private fun EditPaymentMethodDialog(
    provider: PaymentProvider,
    currentDetails: PaymentMethodDetails?,
    onDismiss: () -> Unit,
    onSave: (String, PaymentMethodDetails) -> Unit,
) {
    var name by remember(currentDetails) {
        mutableStateOf(
            currentDetails?.let {
                when (it) {
                    is PaymentMethodDetails.CreditCard -> it.name
                    is PaymentMethodDetails.DebitCard -> it.name
                    is PaymentMethodDetails.Revolut -> it.name
                    is PaymentMethodDetails.Satispay -> it.name
                    is PaymentMethodDetails.Paypal -> it.name
                    is PaymentMethodDetails.Klarna -> it.name
                    is PaymentMethodDetails.Cash -> it.name
                }
            } ?: "",
        )
    }

    when (provider) {
        PaymentProvider.CREDIT_CARD_SALDO,
        PaymentProvider.CREDIT_CARD_REVOLVING,
        -> {
            val cardType = if (provider == PaymentProvider.CREDIT_CARD_SALDO) CreditCardType.SALDO else CreditCardType.REVOLVING
            val details = currentDetails as? PaymentMethodDetails.CreditCard
            var limitText by remember(details) { mutableStateOf(if (details != null) details.limit.toString() else "0") }
            var closingDayText by remember(details) { mutableStateOf(if (details != null) details.closingDay.toString() else "0") }
            var paymentDayText by remember(details) { mutableStateOf(if (details != null) details.paymentDay.toString() else "0") }
            var isLastDayOfMonth by remember(details) {
                mutableStateOf(details?.closingDay == null || details.closingDay >= 31)
            }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(providerLabel(provider)) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.card_name)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = cardType.name, onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.card_type_label)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = limitText, onValueChange = { limitText = it }, label = { Text(stringResource(R.string.card_limit)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isLastDayOfMonth,
                                onCheckedChange = {
                                    isLastDayOfMonth = it
                                    if (it) closingDayText = ""
                                },
                            )
                            Text(stringResource(R.string.end_of_month), style = MaterialTheme.typography.bodySmall)
                        }
                        if (!isLastDayOfMonth) {
                            OutlinedTextField(
                                value = closingDayText,
                                onValueChange = {
                                    closingDayText = it
                                    if (it.isNotEmpty()) isLastDayOfMonth = false
                                },
                                label = { Text(stringResource(R.string.closing_day)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        OutlinedTextField(value = paymentDayText, onValueChange = { paymentDayText = it }, label = { Text(stringResource(R.string.payment_day)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val limit = limitText.toDoubleOrNull() ?: return@TextButton
                        val closingDay = if (isLastDayOfMonth) 31 else (closingDayText.toIntOrNull() ?: return@TextButton)
                        val paymentDay = paymentDayText.toIntOrNull() ?: return@TextButton
                        onSave(name, PaymentMethodDetails.CreditCard(name, cardType, limit, closingDay, paymentDay))
                    }) { Text(stringResource(R.string.save)) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
            )
        }

        PaymentProvider.DEBIT_CARD -> {
            val details = currentDetails as? PaymentMethodDetails.DebitCard
            var issuerText by remember(details) { mutableStateOf(details?.issuer ?: "") }
            var cardNumberText by remember(details) { mutableStateOf(details?.cardNumber ?: "") }
            var notesText by remember(details) { mutableStateOf(details?.notes ?: "") }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(providerLabel(provider)) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.card_name)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = issuerText, onValueChange = { issuerText = it }, label = { Text(stringResource(R.string.issuer_bank)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = cardNumberText, onValueChange = { cardNumberText = it }, label = { Text(stringResource(R.string.card_number)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = notesText, onValueChange = { notesText = it }, label = { Text(stringResource(R.string.notes)) }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onSave(name, PaymentMethodDetails.DebitCard(name, issuerText.ifBlank { null }, cardNumberText.ifBlank { null }, notesText.ifBlank { null }))
                    }) { Text(stringResource(R.string.save)) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
            )
        }

        PaymentProvider.REVOLUT -> {
            val details = currentDetails as? PaymentMethodDetails.Revolut
            var currencyText by remember(details) { mutableStateOf(details?.currency ?: "EUR") }
            var ibanText by remember(details) { mutableStateOf(details?.iban ?: "") }
            var accountNumberText by remember(details) { mutableStateOf(details?.accountNumber ?: "") }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(providerLabel(provider)) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.card_name)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = currencyText, onValueChange = { currencyText = it }, label = { Text(stringResource(R.string.currency)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = ibanText, onValueChange = { ibanText = it }, label = { Text(stringResource(R.string.iban)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = accountNumberText, onValueChange = { accountNumberText = it }, label = { Text(stringResource(R.string.account_number)) }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onSave(name, PaymentMethodDetails.Revolut(name, currencyText, ibanText.ifBlank { null }, accountNumberText.ifBlank { null }))
                    }) { Text(stringResource(R.string.save)) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
            )
        }

        PaymentProvider.SATISPAY -> {
            val details = currentDetails as? PaymentMethodDetails.Satispay
            var weeklyBudgetText by remember(details) { mutableStateOf(if (details != null) details.weeklyBudget.toString() else "0") }
            var sddDayText by remember(details) { mutableStateOf(if (details != null) details.sddDay.toString() else "1") }
            var ibanText by remember(details) { mutableStateOf(details?.iban ?: "") }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(providerLabel(provider)) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.card_name)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = weeklyBudgetText, onValueChange = { weeklyBudgetText = it }, label = { Text(stringResource(R.string.weekly_budget)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = sddDayText, onValueChange = { sddDayText = it }, label = { Text(stringResource(R.string.sdd_day)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = ibanText, onValueChange = { ibanText = it }, label = { Text(stringResource(R.string.iban)) }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val weeklyBudget = weeklyBudgetText.toDoubleOrNull() ?: return@TextButton
                        val sddDay = sddDayText.toIntOrNull() ?: return@TextButton
                        onSave(name, PaymentMethodDetails.Satispay(name, weeklyBudget, sddDay, ibanText.ifBlank { null }))
                    }) { Text(stringResource(R.string.save)) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
            )
        }

        PaymentProvider.PAYPAL -> {
            val details = currentDetails as? PaymentMethodDetails.Paypal
            var emailText by remember(details) { mutableStateOf(details?.email ?: "") }
            var bnplCountText by remember(details) { mutableStateOf(if (details != null) details.bnplInstallmentCount.toString() else "3") }
            var bnplCycleText by remember(details) { mutableStateOf(if (details != null) details.bnplCycleDays.toString() else "14") }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(providerLabel(provider)) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.card_name)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = emailText, onValueChange = { emailText = it }, label = { Text(stringResource(R.string.email)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = bnplCountText, onValueChange = { bnplCountText = it }, label = { Text(stringResource(R.string.bnpl_installments)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = bnplCycleText, onValueChange = { bnplCycleText = it }, label = { Text(stringResource(R.string.bnpl_cycle_days)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val count = bnplCountText.toIntOrNull() ?: return@TextButton
                        val cycle = bnplCycleText.toIntOrNull() ?: return@TextButton
                        onSave(name, PaymentMethodDetails.Paypal(name, emailText, count, cycle))
                    }) { Text(stringResource(R.string.save)) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
            )
        }

        PaymentProvider.KLARNA -> {
            val details = currentDetails as? PaymentMethodDetails.Klarna
            var bnplCountText by remember(details) { mutableStateOf(if (details != null) details.bnplInstallmentCount.toString() else "4") }
            var bnplCycleText by remember(details) { mutableStateOf(if (details != null) details.bnplCycleDays.toString() else "14") }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(providerLabel(provider)) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.card_name)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = bnplCountText, onValueChange = { bnplCountText = it }, label = { Text(stringResource(R.string.bnpl_installments)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = bnplCycleText, onValueChange = { bnplCycleText = it }, label = { Text(stringResource(R.string.bnpl_cycle_days)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val count = bnplCountText.toIntOrNull() ?: return@TextButton
                        val cycle = bnplCycleText.toIntOrNull() ?: return@TextButton
                        onSave(name, PaymentMethodDetails.Klarna(name, count, cycle))
                    }) { Text(stringResource(R.string.save)) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
            )
        }

        PaymentProvider.CASH -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(providerLabel(provider)) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.card_name)) }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onSave(name, PaymentMethodDetails.Cash(name)) }) { Text(stringResource(R.string.save)) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
            )
        }
    }
}

@Composable
private fun providerLabel(provider: PaymentProvider): String {
    return when (provider) {
        PaymentProvider.CASH -> stringResource(R.string.provider_cash)
        PaymentProvider.CREDIT_CARD_SALDO -> stringResource(R.string.provider_credit_card_saldo)
        PaymentProvider.CREDIT_CARD_REVOLVING -> stringResource(R.string.provider_credit_card_revolving)
        PaymentProvider.DEBIT_CARD -> stringResource(R.string.provider_debit_card)
        PaymentProvider.REVOLUT -> stringResource(R.string.provider_revolut)
        PaymentProvider.SATISPAY -> stringResource(R.string.provider_satispay)
        PaymentProvider.PAYPAL -> stringResource(R.string.provider_paypal)
        PaymentProvider.KLARNA -> stringResource(R.string.provider_klarna)
    }
}

private fun providerIcon(provider: PaymentProvider): ImageVector {
    return when (provider) {
        PaymentProvider.CASH -> Icons.Default.Money
        PaymentProvider.CREDIT_CARD_SALDO -> Icons.Default.CreditCard
        PaymentProvider.CREDIT_CARD_REVOLVING -> Icons.Default.CreditCard
        PaymentProvider.DEBIT_CARD -> Icons.Default.CreditCard
        PaymentProvider.REVOLUT -> Icons.Default.Savings
        PaymentProvider.SATISPAY -> Icons.Default.Star
        PaymentProvider.PAYPAL -> Icons.Default.Payment
        PaymentProvider.KLARNA -> Icons.Default.ShoppingCart
    }
}
