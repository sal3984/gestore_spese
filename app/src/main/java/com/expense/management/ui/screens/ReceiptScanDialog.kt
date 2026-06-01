package com.expense.management.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.domain.model.ReceiptScanResult

@Composable
fun ReceiptScanDialog(
    result: ReceiptScanResult,
    currencySymbol: String,
    onApply: () -> Unit,
    onDiscard: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDiscard,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                stringResource(R.string.scan_result_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                if (result.amount != null) {
                    ScanResultRow(
                        label = stringResource(R.string.amount_converted_label, currencySymbol),
                        value = String.format("%.2f %s", result.amount, currencySymbol),
                    )
                }
                if (result.description != null) {
                    ScanResultRow(
                        label = stringResource(R.string.merchant_label),
                        value = result.description,
                    )
                }
                if (result.date != null) {
                    ScanResultRow(
                        label = stringResource(R.string.transaction_date),
                        value = result.date,
                    )
                }
                if (result.amount == null && result.description == null && result.date == null) {
                    Text(
                        stringResource(R.string.scan_no_data),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApply,
                enabled = result.amount != null || result.description != null || result.date != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDiscard,
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.discard))
            }
        },
    )
}

@Composable
private fun ScanResultRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
