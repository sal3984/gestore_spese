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
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.material3.ButtonDefaults
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
fun MonthSelector(selectedDate: LocalDate, onDateChange: (LocalDate) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { onDateChange(selectedDate.minusMonths(1)) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("<") }
        Text(selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")).replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold)
        Button(onClick = { onDateChange(selectedDate.plusMonths(1)) }, enabled = !selectedDate.isAfter(LocalDate.now().plusMonths(1).minusDays(1)), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text(">") }
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
@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    currencySymbol: String,
    dateFormat: String, // NUOVO: Formato data
    onDelete: (Long) -> Unit,
    onEdit: (Long) -> Unit
) {
    val category = getCategory(transaction.categoryId)
    val isIncome = transaction.type == "income"
    val amountColor = if (isIncome) Color(0xFF0F9D58) else MaterialTheme.colorScheme.error
    val effectiveDate = LocalDate.parse(transaction.effectiveDate)

    // Formatta la data usando il formato preferito dall'utente
    val dateFormatter = try {
        DateTimeFormatter.ofPattern(dateFormat)
    } catch (e: Exception) {
        DateTimeFormatter.ofPattern("dd/MM/yyyy") // Fallback
    }
    val formattedDate = effectiveDate.format(dateFormatter)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEdit(transaction.id) },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sezione Icona e Descrizione
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icona Categoria
                Text(
                    text = category.icon,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(4.dp)
                        .wrapContentSize(Alignment.Center)
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    // Descrizione e Categoria
                    Text(
                        transaction.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row {
                        Text(
                            category.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Data Addebito
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "(${formattedDate})", // Usa la data formattata
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        // Tag CC
                        if(transaction.isCreditCard && !isIncome) {
                            Text(
                                " [CC]",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Sezione Importo e Pulsante Elimina
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (isIncome) "+" else "-"} $currencySymbol ${String.format(Locale.ITALIAN, "%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(onClick = { onDelete(transaction.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }
    }
}


fun formatMoney(amount: Double): String = NumberFormat.getCurrencyInstance(Locale.ITALY).format(amount)

// Funzione helper per trovare la categoria, utile per TransactionItem
fun getCategory(id: String) = CATEGORIES.firstOrNull { it.id == id } ?: CATEGORIES.first { it.id == "other" }
