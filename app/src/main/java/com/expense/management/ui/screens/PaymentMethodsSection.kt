package com.expense.management.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.data.CardType
import com.expense.management.data.CreditCardEntity
import java.util.Locale

@Composable
fun paymentMethodsSection(
    currentCurrency: String,
    allCreditCards: List<CreditCardEntity>,
    onAddCreditCard: (CreditCardEntity) -> Unit,
    onUpdateCreditCard: (CreditCardEntity) -> Unit,
    onDeleteCreditCard: (CreditCardEntity) -> Unit,
    onNavigateToFullScreen: () -> Unit = {},
) {
    var showAddCardDialog by remember { mutableStateOf(false) }
    var showEditCardDialog by remember { mutableStateOf<CreditCardEntity?>(null) }
    var showDeleteCardDialog by remember { mutableStateOf<CreditCardEntity?>(null) }

    settingsSectionHeader(stringResource(R.string.credit_card_settings))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column {
            if (allCreditCards.isEmpty()) {
                Text(
                    stringResource(R.string.no_cards_configured),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                allCreditCards.forEach { card ->
                    ListItem(
                        headlineContent = { Text(card.name, fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            Text(
                                "${if (card.type == CardType.SALDO) stringResource(R.string.single_balance) else stringResource(R.string.installment_plan)} • ${stringResource(R.string.max_limit, currentCurrency)} ${String.format(Locale.US, "%.0f", card.limit)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { showEditCardDialog = card }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { showDeleteCardDialog = card }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        },
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }

            Button(
                onClick = { showAddCardDialog = true },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.add_card))
            }

            TextButton(
                onClick = onNavigateToFullScreen,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(stringResource(R.string.manage_payment_methods))
            }
        }
    }

    // Dialog Aggiungi/Modifica Carta
    if (showAddCardDialog || showEditCardDialog != null) {
        val isEditing = showEditCardDialog != null
        val cardToEdit = showEditCardDialog

        var name by remember { mutableStateOf(cardToEdit?.name ?: "") }
        var limit by remember { mutableStateOf(cardToEdit?.limit?.toString() ?: "") }
        var closingDay by remember { mutableStateOf(cardToEdit?.closingDay?.toString() ?: "1") }
        var paymentDay by remember { mutableStateOf(cardToEdit?.paymentDay?.toString() ?: "15") }
        var type by remember { mutableStateOf(cardToEdit?.type ?: CardType.SALDO) }

        AlertDialog(
            onDismissRequest = {
                showAddCardDialog = false
                showEditCardDialog = null
            },
            title = { Text(if (isEditing) stringResource(R.string.edit_card) else stringResource(R.string.add_card)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.card_name)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                    OutlinedTextField(
                        value = limit,
                        onValueChange = { limit = it.replace(',', '.') },
                        label = { Text(stringResource(R.string.card_limit)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )

                    Text(stringResource(R.string.card_type_label), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = type == CardType.SALDO,
                            onClick = { type = CardType.SALDO },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) {
                            Text(stringResource(R.string.single_balance))
                        }
                        SegmentedButton(
                            selected = type == CardType.REVOLVING,
                            onClick = { type = CardType.REVOLVING },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) {
                            Text(stringResource(R.string.installment_plan))
                        }
                    }

                    if (type == CardType.SALDO) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            OutlinedTextField(
                                value = closingDay,
                                onValueChange = { closingDay = it },
                                label = { Text(stringResource(R.string.closing_day)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                            )
                            OutlinedTextField(
                                value = paymentDay,
                                onValueChange = { paymentDay = it },
                                label = { Text(stringResource(R.string.payment_day)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val limitVal = limit.toDoubleOrNull()
                        val closingDayVal = if (type == CardType.SALDO) (closingDay.toIntOrNull() ?: 0) else 0
                        val paymentDayVal = if (type == CardType.SALDO) (paymentDay.toIntOrNull() ?: 0) else 0

                        if (name.isNotBlank() && limitVal != null) {
                            val newCard = CreditCardEntity(
                                id = cardToEdit?.id ?: java.util.UUID.randomUUID().toString(),
                                name = name,
                                limit = limitVal,
                                closingDay = closingDayVal,
                                paymentDay = paymentDayVal,
                                type = type,
                            )
                            if (isEditing) {
                                onUpdateCreditCard(newCard)
                            } else {
                                onAddCreditCard(newCard)
                            }
                            showAddCardDialog = false
                            showEditCardDialog = null
                        }
                    },
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddCardDialog = false
                    showEditCardDialog = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Dialog Elimina Carta
    if (showDeleteCardDialog != null) {
        val cardToDelete = showDeleteCardDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteCardDialog = null },
            title = { Text(stringResource(R.string.delete_card_title)) },
            text = { Text(stringResource(R.string.delete_card_message, cardToDelete.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCreditCard(cardToDelete)
                        showDeleteCardDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCardDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
