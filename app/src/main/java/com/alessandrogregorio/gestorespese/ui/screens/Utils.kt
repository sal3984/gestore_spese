package com.alessandrogregorio.gestorespese.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alessandrogregorio.gestorespese.data.TransactionEntity
import com.alessandrogregorio.gestorespese.ui.screens.category.CATEGORIES
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// --- COMPONENTI GRAFICI RIUTILIZZABILI ---

@Composable
fun MonthSelector(date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onDateChange(date.minusMonths(1)) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PERIODO", style = MaterialTheme.typography.labelSmall)
            Text(date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN)).uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = { onDateChange(date.plusMonths(1)) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
    }
}

@Composable
fun BalanceCard(balance: Double, income: Double, expense: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Saldo Mensile", style = MaterialTheme.typography.bodyMedium)
            Text(
                NumberFormat.getCurrencyInstance(Locale.ITALY).format(balance),
                style = MaterialTheme.typography.displaySmall,
                color = if (balance >= 0) Color.Black else Color.Red,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ComparisonCard(diff: Double) {
    Card(colors = CardDefaults.cardColors(containerColor = if(diff > 0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if(diff > 0) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = if(diff > 0) Color.Red else Color.Green)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if(diff > 0) "Hai speso ${formatMoney(diff)} in più." else "Hai risparmiato ${formatMoney(Math.abs(diff))}!")
        }
    }
}

// Item Singolo Movimento
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItem(t: TransactionEntity, currency: String, onDelete: (Long) -> Unit, onEdit: (Long) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val category = CATEGORIES.find { it.id == t.categoryId } ?: CATEGORIES.last()

    // Usiamo Card per rendere l'intera riga cliccabile in modo più robusto
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEdit(t.id) } // Azione di modifica su click
    ) {
        ListItem(
            headlineContent = { Text(t.description) },
            supportingContent = {
                Text("${category.icon} - Data Addebito: ${t.effectiveDate.format(DateTimeFormatter.ISO_DATE)}")
            },
            leadingContent = {
                Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Text(category.id, fontSize = 20.dp.value.sp)
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = (if(t.type == "expense") "-" else "+") + "$currency ${String.format("%.2f", t.amount)}",
                        color = if(t.type == "expense") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    // Bottone per eliminare - la sua azione è isolata dal click della Card
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = Color.Gray)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Conferma Eliminazione") },
            text = { Text("Sei sicuro di voler eliminare il movimento '${t.description}' di $currency ${String.format("%.2f", t.amount)}?") },
            confirmButton = {
                Button(onClick = {
                    onDelete(t.id)
                    showDeleteDialog = false
                }) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annulla") }
            }
        )
    }
}

fun formatMoney(amount: Double): String = NumberFormat.getCurrencyInstance(Locale.ITALY).format(amount)