package com.expense.management.ui.screens.amex

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.expense.management.domain.model.AmexInstallmentStrategy
import kotlin.math.ceil
import kotlin.math.round

@Composable
fun AmexInstallmentSetupDialog(
    totalAmount: Double,
    currencySymbol: String = "€",
    initialStrategy: AmexInstallmentStrategy? = null,
    onConfirm: (AmexInstallmentStrategy) -> Unit,
    onDismiss: () -> Unit,
) {
    val isFixedAmount = initialStrategy is AmexInstallmentStrategy.FixedAmount
    var mode by remember(initialStrategy) { mutableStateOf(if (isFixedAmount) Mode.FIXED_AMOUNT else Mode.FIXED_DURATION) }
    var amountInput by remember(initialStrategy) {
        mutableStateOf(
            (initialStrategy as? AmexInstallmentStrategy.FixedAmount)?.amount?.toDisplayString() ?: "",
        )
    }
    var durationInput by remember(initialStrategy) {
        mutableStateOf(
            (initialStrategy as? AmexInstallmentStrategy.FixedDuration)?.months?.toString() ?: "",
        )
    }

    val parsedAmount = amountInput.toDoubleOrNull()
    val parsedDuration = durationInput.toIntOrNull()

    val counterpart = when (mode) {
        Mode.FIXED_AMOUNT -> {
            parsedAmount?.takeIf { it > 0 }?.let {
                val months = ceil(totalAmount / it).toInt()
                "≈ $months mesi"
            } ?: ""
        }
        Mode.FIXED_DURATION -> {
            parsedDuration?.takeIf { it > 0 }?.let {
                "≈ ${(totalAmount / it).roundTo2()} $currencySymbol/mese"
            } ?: ""
        }
    }

    val isValid = when (mode) {
        Mode.FIXED_AMOUNT -> parsedAmount != null && parsedAmount > 0
        Mode.FIXED_DURATION -> parsedDuration != null && parsedDuration > 0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Imposta piano rateale Amex") },
        text = {
            Column {
                Text(
                    "Importo totale: ${totalAmount.roundTo2()} $currencySymbol",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.selectable(
                            selected = mode == Mode.FIXED_AMOUNT,
                            role = Role.RadioButton,
                            onClick = { mode = Mode.FIXED_AMOUNT },
                        ),
                    ) {
                        RadioButton(
                            selected = mode == Mode.FIXED_AMOUNT,
                            onClick = null,
                        )
                        Text("Importo rata", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.width(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.selectable(
                            selected = mode == Mode.FIXED_DURATION,
                            role = Role.RadioButton,
                            onClick = { mode = Mode.FIXED_DURATION },
                        ),
                    ) {
                        RadioButton(
                            selected = mode == Mode.FIXED_DURATION,
                            onClick = null,
                        )
                        Text("Numero rate", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(16.dp))
                when (mode) {
                    Mode.FIXED_AMOUNT -> OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                        label = { Text("Importo rata") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Mode.FIXED_DURATION -> OutlinedTextField(
                        value = durationInput,
                        onValueChange = { durationInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Numero di mesi") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                if (counterpart.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        counterpart,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (mode) {
                        Mode.FIXED_AMOUNT -> parsedAmount?.let { onConfirm(AmexInstallmentStrategy.FixedAmount(it)) }
                        Mode.FIXED_DURATION -> parsedDuration?.let { onConfirm(AmexInstallmentStrategy.FixedDuration(it)) }
                    }
                },
                enabled = isValid,
            ) { Text("Conferma") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}

private enum class Mode { FIXED_AMOUNT, FIXED_DURATION }

private fun Double.toDisplayString(): String = String.format(java.util.Locale.getDefault(), "%.2f", this)

private fun Double.roundTo2(): Double = round(this * 100.0) / 100.0
